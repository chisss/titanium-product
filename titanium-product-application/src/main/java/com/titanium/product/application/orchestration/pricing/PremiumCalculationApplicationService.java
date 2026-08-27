package com.titanium.product.application.orchestration.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.aggregate.PremiumCalculation;
import com.titanium.product.application.command.pricing.PremiumCalculationCommand;
import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.exception.PremiumCalculationConcurrentConflictException;
import com.titanium.product.repository.PremiumCalculationRepository;
import com.titanium.product.service.PremiumAdjustmentService;
import com.titanium.product.service.PremiumCalculationBreakdownService;
import com.titanium.product.valueobject.PremiumQuote;
import com.titanium.product.valueobject.pricing.CalculationLine;
import com.titanium.product.valueobject.pricing.CalculationModelExecutionResult;
import com.titanium.product.valueobject.pricing.PremiumAdjustment;
import com.titanium.product.valueobject.pricing.PremiumAdjustmentRequest;
import com.titanium.product.valueobject.pricing.PremiumAdjustmentResult;
import com.titanium.product.valueobject.pricing.PremiumCalculationEvidence;
import com.titanium.product.valueobject.pricing.PricingRoundingRule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Product 保费确认计算应用编排器。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PremiumCalculationApplicationService {

    private final PremiumQuoteApplicationService premiumQuoteApplicationService;
    private final PremiumCalculationRepository premiumCalculationRepository;
    private final PremiumAdjustmentService premiumAdjustmentService;
    private final PremiumCalculationBreakdownService breakdownService;
    private final PricingEvidenceHasher pricingEvidenceHasher;

    /** 执行确认计算并固化不可变事实。 */
    public PremiumCalculation confirm(PremiumCalculationCommand command) {
        validateCommand(command);
        String requestHash = requestHash(command);
        Optional<PremiumCalculation> existing = premiumCalculationRepository.findByIdempotencyKey(
                command.tenantId(), command.calculationRequestId(), command.purpose());
        if (existing.isPresent()) {
            existing.get().assertSameRequest(requestHash);
            return existing.get();
        }

        PremiumQuote quote = premiumQuoteApplicationService.quote(new PremiumQuoteCommand(
                command.tenantId(), command.productId(), command.calculationRequestId(), command.businessTime(),
                command.currency(), command.sumInsured(), command.age(), command.gender(),
                command.paymentTermYears(), command.coverageTermYears(), command.paymentPeriods(),
                command.requestSnapshot(), command.channelId(), command.policyYear()));
        if (!command.productVersion().equals(quote.productVersion())) {
            throw new BusinessException("确认请求产品版本与当前产品不一致",
                    ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED);
        }
        if (command.expectedPricingPlanVersion() != null
                && !command.expectedPricingPlanVersion().equals(quote.pricingPlanVersion())) {
            throw new BusinessException("确认请求计划版本与当前定价计划不一致",
                    ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED);
        }

        PricingRoundingRule roundingRule = new PricingRoundingRule(
                quote.roundingScale(), RoundingMode.valueOf(quote.roundingMode()));
        PremiumAdjustmentResult adjustmentResult = premiumAdjustmentService.apply(
                quote.totalPremium(), command.underwritingAdjustments(), roundingRule);
        CalculationModelExecutionResult breakdown = breakdownService.applyAdjustments(
                quote.calculationLines(), quote.totalPremium(), adjustmentResult.adjustments(), quote.currency());
        if (breakdown.totals().customerPayable().compareTo(adjustmentResult.totalPremium()) != 0) {
            throw new BusinessException("结构化费用汇总与核保调整结果不一致",
                    ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED);
        }
        String adjustmentHash = pricingEvidenceHasher.hash(canonicalAdjustments(command.underwritingAdjustments()));
        String inputHash = pricingEvidenceHasher.hash(quote.inputHash() + '|' + adjustmentHash);
        String resultHash = pricingEvidenceHasher.hash(String.join(
                "|", quote.resultHash(), adjustmentResult.standardPremium().toPlainString(),
                canonicalAppliedAdjustments(adjustmentResult.adjustments()),
                adjustmentResult.totalPremium().toPlainString(), canonicalBreakdown(breakdown),
                quote.pricingPlanContentHash()));
        PremiumCalculation calculation = PremiumCalculation.confirm(
                UUID.randomUUID().toString(), command.calculationRequestId(), command.bizNo(), command.purpose(),
                command.tenantId(), command.productId(), command.businessTime(), quote.currency(),
                adjustmentResult.standardPremium(), adjustmentResult.totalPremium(),
                installment(adjustmentResult.totalPremium(), command.paymentPeriods(), roundingRule),
                command.paymentPeriods(), adjustmentResult.adjustments(), breakdown.totals(), breakdown.lines(),
                evidence(command, quote),
                command.requestSnapshot(), requestHash, inputHash, resultHash, LocalDateTime.now());

        try {
            premiumCalculationRepository.save(calculation);
            log.info("Product确认计算完成: calculationId={}, bizNo={}, requestId={}, totalPremium={}",
                    calculation.getCalculationId(), command.bizNo(), command.calculationRequestId(),
                    calculation.getTotalPremium());
            return calculation;
        } catch (PremiumCalculationConcurrentConflictException exception) {
            return premiumCalculationRepository.findByIdempotencyKey(
                            command.tenantId(), command.calculationRequestId(), command.purpose())
                    .map(winner -> {
                        winner.assertSameRequest(requestHash);
                        return winner;
                    })
                    .orElseThrow(() -> exception);
        }
    }

    private PremiumCalculationEvidence evidence(PremiumCalculationCommand command, PremiumQuote quote) {
        return new PremiumCalculationEvidence(
                command.productVersion(), quote.pricingPlanVersion(), quote.pricingPlanContentHash(),
                quote.rateTableCode(), quote.rateTableVersion(), quote.rateTableContentHash(),
                quote.featureSnapshotId(), quote.ruleArtifactCode(), quote.ruleArtifactVersion(),
                quote.ruleArtifactHash(), quote.roundingScale(), quote.roundingMode(),
                quote.calculationModelCode(), quote.calculationModelVersion(), quote.calculationModelHash(),
                quote.dynamicFactorEvidence());
    }

    private String requestHash(PremiumCalculationCommand command) {
        String legacyPayload = String.join(
                "|", command.tenantId(), command.productId(), command.calculationRequestId(), command.bizNo(),
                command.purpose().getCode(), command.productVersion(), command.businessTime().toString(),
                command.currency().trim().toUpperCase(Locale.ROOT), command.sumInsured().stripTrailingZeros().toPlainString(),
                Integer.toString(command.age()), command.gender(), Integer.toString(command.paymentTermYears()),
                Integer.toString(command.coverageTermYears()), Integer.toString(command.paymentPeriods()),
                command.channelId() == null ? "*" : command.channelId(), Integer.toString(command.policyYear()),
                pricingEvidenceHasher.canonicalValue(command.requestSnapshot()),
                canonicalAdjustments(command.underwritingAdjustments()));
        if (command.expectedPricingPlanVersion() == null) {
            return pricingEvidenceHasher.hash(legacyPayload);
        }
        return pricingEvidenceHasher.hash(
                legacyPayload + "|EXPECTED_PLAN|" + command.expectedPricingPlanVersion());
    }

    private String canonicalAdjustments(List<PremiumAdjustmentRequest> requests) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (PremiumAdjustmentRequest request : requests) {
            joiner.add(String.join(":", request.adjustmentCode(), request.type().getCode(),
                    request.value().stripTrailingZeros().toPlainString(),
                    request.reason() == null ? "*" : request.reason(),
                    request.ruleVersion() == null ? "*" : request.ruleVersion()));
        }
        return joiner.toString();
    }

    private String canonicalAppliedAdjustments(List<PremiumAdjustment> adjustments) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (PremiumAdjustment adjustment : adjustments) {
            joiner.add(String.join(":", adjustment.adjustmentCode(), adjustment.type().getCode(),
                    adjustment.adjustmentAmount().toPlainString(), adjustment.premiumAfter().toPlainString()));
        }
        return joiner.toString();
    }

    private String canonicalBreakdown(CalculationModelExecutionResult breakdown) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (CalculationLine line : breakdown.lines()) {
            joiner.add(String.join(":", line.lineId(), line.componentCode(), line.componentVersion(),
                    line.amountChannel().name(), line.direction().name(),
                    line.calculatedAmount().stripTrailingZeros().toPlainString()));
        }
        return String.join("|", breakdown.totals().customerPayable().stripTrailingZeros().toPlainString(),
                breakdown.totals().internalCostTotal().stripTrailingZeros().toPlainString(), joiner.toString());
    }

    private BigDecimal installment(BigDecimal total, int periods, PricingRoundingRule roundingRule) {
        return total.divide(BigDecimal.valueOf(periods), roundingRule.scale(), roundingRule.roundingMode());
    }

    private void validateCommand(PremiumCalculationCommand command) {
        if (command == null || blank(command.tenantId()) || blank(command.productId())
                || blank(command.calculationRequestId()) || blank(command.bizNo())
                || unsupportedPurpose(command.purpose())
                || blank(command.productVersion()) || command.businessTime() == null || blank(command.currency())
                || command.sumInsured() == null || command.sumInsured().signum() <= 0
                || command.age() < 0 || command.age() > 120 || blank(command.gender())
                || command.paymentTermYears() <= 0 || command.coverageTermYears() <= 0
                || command.paymentPeriods() <= 0 || hasInvalidAdjustment(command)) {
            throw new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
        }
    }

    private boolean hasInvalidAdjustment(PremiumCalculationCommand command) {
        return command.underwritingAdjustments().stream().anyMatch(adjustment -> adjustment == null
                || blank(adjustment.adjustmentCode()) || adjustment.type() == null
                || adjustment.value() == null || adjustment.value().signum() < 0);
    }

    private boolean unsupportedPurpose(PricingCalculationPurpose purpose) {
        return purpose != PricingCalculationPurpose.ISSUANCE_CONFIRM
                && purpose != PricingCalculationPurpose.MAINTENANCE;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
