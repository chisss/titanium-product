package com.titanium.product.application.orchestration.pricing.surrender;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.aggregate.PremiumCalculation;
import com.titanium.product.aggregate.lifecycle.PremiumLifecycleAdjustment;
import com.titanium.product.aggregate.surrender.SurrenderValuePolicy;
import com.titanium.product.application.command.pricing.lifecycle.CreatePremiumLifecycleAdjustmentCommand;
import com.titanium.product.application.command.pricing.surrender.CalculateSurrenderValueCommand;
import com.titanium.product.application.model.pricing.surrender.SurrenderValueCalculationResult;
import com.titanium.product.application.orchestration.pricing.PricingEvidenceHasher;
import com.titanium.product.application.orchestration.pricing.lifecycle.PremiumLifecycleAdjustmentApplicationService;
import com.titanium.product.common.enums.PremiumLifecycleType;
import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.exception.PremiumCalculationConcurrentConflictException;
import com.titanium.product.repository.PremiumCalculationRepository;
import com.titanium.product.repository.SurrenderValuePolicyRepository;
import com.titanium.product.valueobject.pricing.CalculationLine;
import com.titanium.product.valueobject.pricing.CalculationTotals;
import com.titanium.product.valueobject.pricing.surrender.SurrenderValueOutcome;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 退保价值确认应用编排器。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SurrenderValueApplicationService {

    private static final String REPLACEMENT_SUFFIX = ":replacement";
    private static final String ADJUSTMENT_SUFFIX = ":adjustment";

    private final PremiumCalculationRepository calculationRepository;
    private final SurrenderValuePolicyRepository surrenderValuePolicyRepository;
    private final PremiumLifecycleAdjustmentApplicationService lifecycleAdjustmentApplicationService;
    private final PricingEvidenceHasher evidenceHasher;

    public SurrenderValueCalculationResult calculate(CalculateSurrenderValueCommand command) {
        validateCommand(command);
        PremiumCalculation original = calculationRepository.findById(
                        command.tenantId(), command.originalCalculationId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRICING_CALCULATION_NOT_FOUND));
        validateOriginal(command, original);
        SurrenderValuePolicy policy = surrenderValuePolicyRepository.findPublished(
                        command.tenantId(), original.getProductId(), command.policyYear(), command.businessTime())
                .orElseThrow(() -> new BusinessException(
                        "未找到业务时点适用的已发布退保价值策略",
                        ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED));

        RoundingMode roundingMode = RoundingMode.valueOf(original.getEvidence().roundingMode());
        int scale = original.getEvidence().roundingScale();
        SurrenderValueOutcome outcome = policy.calculate(
                original.getCalculationTotals().customerPayable(), command.policyEffectiveDate(),
                command.surrenderDate(), scale, roundingMode);
        String requestHash = requestHash(command, original, policy);
        PremiumCalculation replacement = findOrCreateReplacement(
                command, original, policy, outcome, requestHash, scale, roundingMode);
        PremiumLifecycleAdjustment adjustment = lifecycleAdjustmentApplicationService.create(
                new CreatePremiumLifecycleAdjustmentCommand(
                        command.tenantId(), command.surrenderRequestId() + ADJUSTMENT_SUFFIX,
                        command.originalBizNo(),
                        PremiumLifecycleType.SURRENDER, original.getCalculationId(), replacement.getCalculationId(),
                        command.businessTime(), command.reason()));

        if (adjustment.getCustomerAmount().compareTo(outcome.refundAmount()) != 0) {
            throw new BusinessException(
                    "退保价值与生命周期客户贷项无法勾稽",
                    ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED);
        }
        log.info("Product退保价值已确认: requestId={}, adjustmentId={}, refundType={}, amount={}",
                command.surrenderRequestId(), adjustment.getAdjustmentId(), outcome.refundType(), outcome.refundAmount());
        return toResult(command, policy, outcome, replacement, adjustment);
    }

    private PremiumCalculation findOrCreateReplacement(
            CalculateSurrenderValueCommand command,
            PremiumCalculation original,
            SurrenderValuePolicy policy,
            SurrenderValueOutcome outcome,
            String requestHash,
            int scale,
            RoundingMode roundingMode) {
        String calculationRequestId = command.surrenderRequestId() + REPLACEMENT_SUFFIX;
        Optional<PremiumCalculation> existing = calculationRepository.findByIdempotencyKey(
                command.tenantId(), calculationRequestId, PricingCalculationPurpose.MAINTENANCE);
        if (existing.isPresent()) {
            existing.get().assertSameRequest(requestHash);
            return existing.get();
        }

        BigDecimal customerRetentionRate = BigDecimal.ONE.subtract(outcome.cashValueRate());
        CalculationTotals totals = replacementTotals(
                original.getCalculationTotals(), outcome.retainedCustomerAmount(), customerRetentionRate,
                outcome.internalCostRetentionRate(), scale, roundingMode);
        List<CalculationLine> lines = replacementLines(
                original.getCalculationLines(), totals.customerPayable(), customerRetentionRate,
                outcome.internalCostRetentionRate(), scale, roundingMode);
        Map<String, Object> snapshot = surrenderSnapshot(command, policy, outcome);
        String inputHash = evidenceHasher.hash(String.join("|", original.getResultHash(), policy.getContentHash(),
                command.policyEffectiveDate().toString(), command.surrenderDate().toString(),
                Integer.toString(command.policyYear())));
        String resultHash = resultHash(original, policy, outcome, totals, lines);
        PremiumCalculation replacement = PremiumCalculation.confirm(
                UUID.randomUUID().toString(), calculationRequestId, command.originalBizNo(),
                PricingCalculationPurpose.MAINTENANCE, command.tenantId(), original.getProductId(),
                command.businessTime(), original.getCurrency(), totals.premiumSubtotal(), totals.customerPayable(),
                totals.customerPayable().divide(BigDecimal.valueOf(original.getPeriods()), scale, roundingMode),
                original.getPeriods(), List.of(), totals, lines, original.getEvidence(), snapshot,
                requestHash, inputHash, resultHash, LocalDateTime.now());
        try {
            calculationRepository.save(replacement);
            return replacement;
        } catch (PremiumCalculationConcurrentConflictException exception) {
            return calculationRepository.findByIdempotencyKey(
                            command.tenantId(), calculationRequestId, PricingCalculationPurpose.MAINTENANCE)
                    .map(winner -> {
                        winner.assertSameRequest(requestHash);
                        return winner;
                    })
                    .orElseThrow(() -> exception);
        }
    }

    private CalculationTotals replacementTotals(
            CalculationTotals original,
            BigDecimal retainedCustomerAmount,
            BigDecimal customerRetentionRate,
            BigDecimal internalRetentionRate,
            int scale,
            RoundingMode roundingMode) {
        BigDecimal retainedTax = scaled(original.taxAndLevyTotal(), customerRetentionRate, scale, roundingMode)
                .min(retainedCustomerAmount);
        BigDecimal retainedPremium = retainedCustomerAmount.subtract(retainedTax);
        BigDecimal retainedInternal = scaled(
                original.internalCostTotal(), internalRetentionRate, scale, roundingMode);
        return new CalculationTotals(retainedPremium, retainedTax, retainedCustomerAmount, retainedInternal);
    }

    private List<CalculationLine> replacementLines(
            List<CalculationLine> originals,
            BigDecimal targetCustomerAmount,
            BigDecimal customerRetentionRate,
            BigDecimal internalRetentionRate,
            int scale,
            RoundingMode roundingMode) {
        List<CalculationLine> lines = new ArrayList<>();
        for (CalculationLine original : originals) {
            BigDecimal factor = original.amountChannel() == AmountChannel.CUSTOMER_PRICE
                    ? customerRetentionRate
                    : internalRetentionRate;
            lines.add(copyWithAmount(original, scaled(original.calculatedAmount(), factor, scale, roundingMode)));
        }
        rebalanceCustomerLines(lines, targetCustomerAmount);
        return List.copyOf(lines);
    }

    private void rebalanceCustomerLines(List<CalculationLine> lines, BigDecimal target) {
        BigDecimal actual = lines.stream()
                .filter(CalculationLine::affectsCustomerPayable)
                .map(CalculationLine::signedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainder = target.subtract(actual);
        if (remainder.signum() == 0) {
            return;
        }
        for (int index = lines.size() - 1; index >= 0; index--) {
            CalculationLine line = lines.get(index);
            if (!line.affectsCustomerPayable()) {
                continue;
            }
            BigDecimal adjusted = line.direction() == ChargeDirection.DEBIT
                    ? line.calculatedAmount().add(remainder)
                    : line.calculatedAmount().subtract(remainder);
            if (adjusted.signum() >= 0) {
                lines.set(index, copyWithAmount(line, adjusted));
                return;
            }
        }
        throw new BusinessException(
                "退保后客户费用明细无法分摊尾差",
                ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED);
    }

    private CalculationLine copyWithAmount(CalculationLine source, BigDecimal amount) {
        return new CalculationLine(
                source.lineId(), source.componentCode(), source.componentVersion(), source.category(),
                source.amountChannel(), source.direction(), source.payerType(), source.accountingClass(),
                source.currency(), source.baseAmount(), source.rate(), amount, source.nodeCode(),
                source.customerVisible(), source.description(), source.affectsCustomerPayable(),
                source.taxEvidence(), source.commissionEvidence());
    }

    private Map<String, Object> surrenderSnapshot(
            CalculateSurrenderValueCommand command,
            SurrenderValuePolicy policy,
            SurrenderValueOutcome outcome) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("surrenderPolicyCode", policy.getPolicyCode());
        snapshot.put("surrenderPolicyVersion", policy.getPolicyVersion());
        snapshot.put("surrenderPolicyHash", policy.getContentHash());
        snapshot.put("policyYear", command.policyYear());
        snapshot.put("coolingOffDays", policy.getCoolingOffDays());
        snapshot.put("policyEffectiveDate", command.policyEffectiveDate().toString());
        snapshot.put("surrenderDate", command.surrenderDate().toString());
        snapshot.put("refundType", outcome.refundType().name());
        snapshot.put("cashValueRate", outcome.cashValueRate());
        snapshot.put("refundAmount", outcome.refundAmount());
        snapshot.put("internalCostRetentionRate", outcome.internalCostRetentionRate());
        return snapshot;
    }

    private String requestHash(
            CalculateSurrenderValueCommand command,
            PremiumCalculation original,
            SurrenderValuePolicy policy) {
        return evidenceHasher.hash(String.join("|", command.tenantId(), command.surrenderRequestId(),
                command.bizNo(), command.originalBizNo(), original.getCalculationId(), original.getResultHash(),
                policy.getPolicyCode(),
                policy.getPolicyVersion(), policy.getContentHash(), command.policyEffectiveDate().toString(),
                command.surrenderDate().toString(), Integer.toString(command.policyYear()),
                command.businessTime().toString(), command.reason()));
    }

    private String resultHash(
            PremiumCalculation original,
            SurrenderValuePolicy policy,
            SurrenderValueOutcome outcome,
            CalculationTotals totals,
            List<CalculationLine> lines) {
        StringJoiner lineEvidence = new StringJoiner(";");
        lines.forEach(line -> lineEvidence.add(String.join(",", line.lineId(), line.componentCode(),
                line.direction().getCode(), line.calculatedAmount().stripTrailingZeros().toPlainString())));
        return evidenceHasher.hash(String.join("|", original.getResultHash(), policy.getContentHash(),
                outcome.refundType().name(), outcome.cashValueRate().stripTrailingZeros().toPlainString(),
                outcome.refundAmount().stripTrailingZeros().toPlainString(),
                totals.customerPayable().stripTrailingZeros().toPlainString(),
                totals.internalCostTotal().stripTrailingZeros().toPlainString(), lineEvidence.toString()));
    }

    private SurrenderValueCalculationResult toResult(
            CalculateSurrenderValueCommand command,
            SurrenderValuePolicy policy,
            SurrenderValueOutcome outcome,
            PremiumCalculation replacement,
            PremiumLifecycleAdjustment adjustment) {
        return new SurrenderValueCalculationResult(
                command.surrenderRequestId(), policy.getPolicyCode(), policy.getPolicyVersion(),
                policy.getContentHash(), command.policyYear(), policy.getCoolingOffDays(), outcome.refundType(),
                outcome.withinCoolingOff(), outcome.cashValueRate(), outcome.refundAmount(),
                outcome.retainedCustomerAmount(), outcome.internalCostRetentionRate(),
                adjustment.getRequestHash(), adjustment.getOriginalResultHash(),
                adjustment.getReplacementResultHash(), replacement.getEvidence().pricingPlanVersion(),
                replacement.getEvidence().pricingPlanContentHash(), adjustment);
    }

    private BigDecimal scaled(BigDecimal amount, BigDecimal factor, int scale, RoundingMode roundingMode) {
        return amount.multiply(factor).setScale(scale, roundingMode);
    }

    private void validateCommand(CalculateSurrenderValueCommand command) {
        if (command == null || blank(command.tenantId()) || blank(command.surrenderRequestId())
                || blank(command.bizNo()) || blank(command.originalBizNo()) || blank(command.originalCalculationId())
                || command.policyEffectiveDate() == null || command.surrenderDate() == null
                || command.surrenderDate().isBefore(command.policyEffectiveDate())
                || command.policyYear() == null || command.policyYear() < 1 || command.businessTime() == null
                || blank(command.reason())) {
            throw new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
        }
    }

    private void validateOriginal(CalculateSurrenderValueCommand command, PremiumCalculation original) {
        if (original.getPurpose() != PricingCalculationPurpose.ISSUANCE_CONFIRM
                || !Objects.equals(original.getBizNo(), command.originalBizNo())) {
            throw new BusinessException(
                    "退保价值必须基于同一保单的出单确认计算",
                    ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED);
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
