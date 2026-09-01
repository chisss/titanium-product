package com.titanium.product.application.service.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.application.orchestration.pricing.PricingCalculationInput;
import com.titanium.product.application.orchestration.pricing.PricingCalculationOutcome;
import com.titanium.product.application.orchestration.pricing.PricingEvidenceHasher;
import com.titanium.product.application.orchestration.pricing.PricingPlanCalculator;
import com.titanium.product.application.service.pricing.validation.PremiumQuoteCommandValidator;
import com.titanium.product.command.pricing.PremiumQuoteCommand;
import com.titanium.product.pricing.aggregate.PricingPlanDefinition;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.service.ProductQueryService;
import com.titanium.product.repository.PricingPlanRepository;
import com.titanium.product.repository.RateTableSnapshotRepository;
import com.titanium.product.service.CalculationModelExecutionService;
import com.titanium.product.service.PremiumCompositionService;
import com.titanium.product.service.RateTableMatchingService;
import com.titanium.product.valueobject.pricing.calculation.CalculationLine;
import com.titanium.product.valueobject.pricing.calculation.CalculationModelExecutionResult;
import com.titanium.product.valueobject.pricing.premium.PremiumQuote;
import com.titanium.product.valueobject.pricing.pricing.PricingRoundingRule;
import com.titanium.product.valueobject.rate.RateTableCriteria;
import com.titanium.product.valueobject.rate.RateTableRef;
import com.titanium.product.valueobject.rate.RateTableRow;
import com.titanium.product.valueobject.rate.RateTableSnapshot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Product 保费试算应用服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PremiumQuoteApplicationService {

    private final ProductQueryService productQueryService;
    private final PricingPlanRepository pricingPlanRepository;
    private final PricingPlanCalculator pricingPlanCalculator;
    private final RateTableSnapshotRepository rateTableSnapshotRepository;
    private final RateTableMatchingService rateTableMatchingService;
    private final PremiumCompositionService premiumCompositionService;
    private final PricingEvidenceHasher pricingEvidenceHasher;
    private final CalculationModelExecutionService calculationModelExecutionService;
    private final PremiumQuoteCommandValidator premiumQuoteCommandValidator;

    /**
     * 执行不落账的保费试算。
     */
    @Transactional(readOnly = true)
    public PremiumQuote quote(PremiumQuoteCommand command) {
        premiumQuoteCommandValidator.validate(command);
        ProductQueryResult product = productQueryService.findProductById(command.productId(), command.tenantId());
        validateProductAvailability(product, command.productId());

        return pricingPlanRepository.findEffective(
                        command.tenantId(), command.productId(), normalizeCurrency(command.currency()), command.businessTime())
                .map(plan -> quoteWithPricingPlan(command, product, plan))
                .orElseGet(() -> quoteWithLegacyRateTable(command, product));
    }

    private PremiumQuote quoteWithPricingPlan(
            PremiumQuoteCommand command, ProductQueryResult product, PricingPlanDefinition plan) {
        validatePricingPlanBinding(product, plan);
        PricingCalculationOutcome calculation = pricingPlanCalculator.calculatePublished(
                plan,
                new PricingCalculationInput(
                        command.tenantId(), command.productId(), command.requestId(), command.businessTime(),
                        command.currency(), command.sumInsured(), command.age(), command.gender(),
                        command.paymentTermYears(), command.coverageTermYears(), command.paymentPeriods(),
                        command.requestSnapshot(), command.channelId(), command.policyYear()),
                false);
        BigDecimal installment = installment(calculation.totalPremium(), command.paymentPeriods());
        String inputHash = pricingEvidenceHasher.hash(canonicalInput(command, plan));
        String resultHash = pricingEvidenceHasher.hash(String.join(
                "|", inputHash, calculation.totalPremium().toPlainString(), canonicalBreakdown(calculation.breakdown()),
                nullable(calculation.matchedRateRowId()), nullable(calculation.rateTableContentHash()),
                nullable(calculation.featureSnapshotId()), nullable(calculation.ruleArtifactHash()),
                nullable(calculation.calculationModelHash()),
                pricingEvidenceHasher.canonicalValue(calculation.dynamicFactorEvidence())));

        log.info("Product保费试算完成: requestId={}, productId={}, planVersion={}, tableVersion={}, artifactVersion={}",
                command.requestId(), command.productId(), plan.planVersion(), calculation.rateTableVersion(),
                calculation.ruleArtifactVersion());
        return new PremiumQuote(
                UUID.randomUUID().toString(), command.requestId(), command.productId(), product.getVersion(),
                normalizeCurrency(command.currency()), calculation.totalPremium(), installment,
                command.paymentPeriods(), calculation.matchedRate(), calculation.matchedRateRowId(),
                calculation.rateTableCode(), calculation.rateTableVersion(), calculation.rateTableContentHash(),
                plan.planVersion(), plan.contentHash(), calculation.featureSnapshotId(),
                calculation.ruleArtifactCode(), calculation.ruleArtifactVersion(), calculation.ruleArtifactHash(),
                plan.roundingRule().scale(), plan.roundingRule().roundingMode().name(),
                inputHash, resultHash, calculation.breakdown().totals(), calculation.breakdown().lines(),
                calculation.calculationModelCode(), calculation.calculationModelVersion(),
                calculation.calculationModelHash(), calculation.dynamicFactorEvidence());
    }

    private PremiumQuote quoteWithLegacyRateTable(PremiumQuoteCommand command, ProductQueryResult product) {
        validateLegacyPricing(product);

        RateTableCriteria criteria = new RateTableCriteria(
                command.age(), command.gender(), command.paymentTermYears(), command.coverageTermYears());
        RateTableRef rateTableRef = product.getRateTableRef();
        RateTableSnapshot snapshot = rateTableSnapshotRepository.findEffectiveSnapshot(
                        command.tenantId(), command.productId(), rateTableRef.tableCode(), rateTableRef.version(),
                        command.businessTime(), criteria)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.RATE_TABLE_NOT_EFFECTIVE));
        validateSnapshot(command, snapshot);

        RateTableRow matchedRow = rateTableMatchingService.match(snapshot, criteria);
        BigDecimal totalPremium = premiumCompositionService.calculate(
                command.sumInsured(), snapshot.rateUnit(), matchedRow);
        PricingRoundingRule roundingRule = new PricingRoundingRule(2, RoundingMode.HALF_UP);
        CalculationModelExecutionResult breakdown = calculationModelExecutionService.legacy(
                totalPremium, normalizeCurrency(command.currency()), roundingRule);
        BigDecimal installment = installment(totalPremium, command.paymentPeriods());
        String inputHash = pricingEvidenceHasher.hash(canonicalLegacyInput(command, criteria, snapshot));
        String resultHash = pricingEvidenceHasher.hash(
                inputHash + '|' + matchedRow.rowId() + '|' + totalPremium.toPlainString());

        log.info("Product保费试算完成: requestId={}, productId={}, tableCode={}, tableVersion={}, rowId={}",
                command.requestId(), command.productId(), snapshot.tableCode(), snapshot.tableVersion(),
                matchedRow.rowId());
        return new PremiumQuote(
                UUID.randomUUID().toString(), command.requestId(), command.productId(), product.getVersion(),
                normalizeCurrency(command.currency()), totalPremium, installment,
                command.paymentPeriods(), matchedRow.rate(), matchedRow.rowId(),
                snapshot.tableCode(), snapshot.tableVersion(), snapshot.contentHash(),
                null, null, null, null, null, null, 2, RoundingMode.HALF_UP.name(), inputHash, resultHash,
                breakdown.totals(), breakdown.lines(), null, null, null);
    }

    private void validateProductAvailability(ProductQueryResult product, String productId) {
        if (product == null) {
            throw new BusinessException("产品不存在: " + productId, ProductErrorCode.PRODUCT_NOT_EXIST);
        }
        if (!ProductEnum.ProductStatus.EFFECTIVE.equals(product.getStatus())) {
            throw new BusinessException(ProductErrorCode.PRICING_PLAN_NOT_EFFECTIVE);
        }
    }

    private void validatePricingPlanBinding(ProductQueryResult product, PricingPlanDefinition plan) {
        if (isBlank(product.getVersion()) || !product.getVersion().equals(plan.productVersion())
                || product.getPricingMode() != null && product.getPricingMode() != plan.mode()) {
            throw new BusinessException("定价方案与当前产品版本或定价模式不一致",
                    ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED);
        }
    }

    private void validateLegacyPricing(ProductQueryResult product) {
        if (!PricingMode.RATE_TABLE.equals(product.getPricingMode())) {
            throw new BusinessException(ProductErrorCode.PRICING_MODE_UNSUPPORTED);
        }
        RateTableRef ref = product.getRateTableRef();
        if (ref == null || isBlank(ref.tableCode()) || isBlank(ref.version())) {
            throw new BusinessException(ProductErrorCode.PRICING_PLAN_NOT_EFFECTIVE);
        }
    }

    private void validateSnapshot(PremiumQuoteCommand command, RateTableSnapshot snapshot) {
        if (!snapshot.isEffectiveAt(command.businessTime())) {
            throw new BusinessException(ProductErrorCode.RATE_TABLE_NOT_EFFECTIVE);
        }
        if (!normalizeCurrency(command.currency()).equals(normalizeCurrency(snapshot.currency()))) {
            throw new BusinessException(ProductErrorCode.PRICING_CURRENCY_MISMATCH);
        }
    }

    private String canonicalLegacyInput(
            PremiumQuoteCommand command, RateTableCriteria criteria, RateTableSnapshot snapshot) {
        return String.join("|",
                command.tenantId(), command.productId(), command.businessTime().toString(),
                normalizeCurrency(command.currency()), command.sumInsured().stripTrailingZeros().toPlainString(),
                Integer.toString(criteria.age()), criteria.gender(),
                Integer.toString(criteria.paymentTermYears()), Integer.toString(criteria.coverageTermYears()),
                Integer.toString(command.paymentPeriods()), snapshot.tableId(), snapshot.tableVersion(),
                snapshot.contentHash(), pricingEvidenceHasher.canonicalValue(command.requestSnapshot()));
    }

    private String canonicalInput(PremiumQuoteCommand command, PricingPlanDefinition plan) {
        return String.join("|",
                command.tenantId(), command.productId(), command.businessTime().toString(),
                normalizeCurrency(command.currency()), command.sumInsured().stripTrailingZeros().toPlainString(),
                Integer.toString(command.age()), command.gender(), Integer.toString(command.paymentTermYears()),
                Integer.toString(command.coverageTermYears()), Integer.toString(command.paymentPeriods()),
                nullable(command.channelId()), Integer.toString(command.policyYear()),
                plan.productVersion(), plan.planVersion(), plan.contentHash(),
                pricingEvidenceHasher.canonicalValue(command.requestSnapshot()));
    }

    private BigDecimal installment(BigDecimal totalPremium, int paymentPeriods) {
        return totalPremium.divide(BigDecimal.valueOf(paymentPeriods), 2, RoundingMode.HALF_UP);
    }

    private String normalizeCurrency(String currency) {
        return currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String nullable(Object value) {
        return value == null ? "*" : value.toString();
    }

    private String canonicalBreakdown(CalculationModelExecutionResult breakdown) {
        StringJoiner lines = new StringJoiner(",", "[", "]");
        for (CalculationLine line : breakdown.lines()) {
            lines.add(String.join(":", line.lineId(), line.componentCode(), line.componentVersion(),
                    line.category().name(), line.amountChannel().name(), line.direction().name(),
                    line.calculatedAmount().stripTrailingZeros().toPlainString()));
        }
        return String.join("|", breakdown.totals().premiumSubtotal().stripTrailingZeros().toPlainString(),
                breakdown.totals().taxAndLevyTotal().stripTrailingZeros().toPlainString(),
                breakdown.totals().customerPayable().stripTrailingZeros().toPlainString(),
                breakdown.totals().internalCostTotal().stripTrailingZeros().toPlainString(), lines.toString());
    }
}
