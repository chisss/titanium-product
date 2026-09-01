package com.titanium.product.command.pricing;

import java.time.LocalDateTime;
import java.util.List;

import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.product.valueobject.pricing.calculation.CalculationModelRef;
import com.titanium.product.valueobject.pricing.commission.CommissionSchemeRef;
import com.titanium.product.valueobject.pricing.premium.TaxPolicyRef;
import com.titanium.product.valueobject.pricing.pricing.DynamicFactorRef;
import com.titanium.product.valueobject.pricing.pricing.PricingFeatureContract;
import com.titanium.product.valueobject.pricing.pricing.PricingRoundingRule;
import com.titanium.product.valueobject.pricing.pricing.PricingRuleArtifactRef;
import com.titanium.product.valueobject.rate.RateTableRef;

/**
 * 创建定价方案草稿命令。
 */
public record CreatePricingPlanDraftCommand(
        String tenantId,
        String productId,
        String productVersion,
        String planVersion,
        PricingMode mode,
        String currency,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        RateTableRef rateTableRef,
        PricingFeatureContract featureContract,
        PricingRuleArtifactRef artifactRef,
        CalculationModelRef calculationModelRef,
        PricingRoundingRule roundingRule,
        List<TaxPolicyRef> taxPolicyRefs,
        List<CommissionSchemeRef> commissionSchemeRefs,
        List<DynamicFactorRef> dynamicFactorRefs) {

    public CreatePricingPlanDraftCommand {
        taxPolicyRefs = taxPolicyRefs == null ? List.of() : List.copyOf(taxPolicyRefs);
        commissionSchemeRefs = commissionSchemeRefs == null ? List.of() : List.copyOf(commissionSchemeRefs);
        dynamicFactorRefs = dynamicFactorRefs == null ? List.of() : List.copyOf(dynamicFactorRefs);
    }

    public CreatePricingPlanDraftCommand(
            String tenantId,
            String productId,
            String productVersion,
            String planVersion,
            PricingMode mode,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            RateTableRef rateTableRef,
            PricingFeatureContract featureContract,
            PricingRuleArtifactRef artifactRef,
            CalculationModelRef calculationModelRef,
            PricingRoundingRule roundingRule,
            List<TaxPolicyRef> taxPolicyRefs,
            List<CommissionSchemeRef> commissionSchemeRefs) {
        this(tenantId, productId, productVersion, planVersion, mode, currency, effectiveFrom, effectiveTo,
                rateTableRef, featureContract, artifactRef, calculationModelRef, roundingRule, taxPolicyRefs,
                commissionSchemeRefs, List.of());
    }

    public CreatePricingPlanDraftCommand(
            String tenantId,
            String productId,
            String productVersion,
            String planVersion,
            PricingMode mode,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            RateTableRef rateTableRef,
            PricingFeatureContract featureContract,
            PricingRuleArtifactRef artifactRef,
            CalculationModelRef calculationModelRef,
            PricingRoundingRule roundingRule,
            List<TaxPolicyRef> taxPolicyRefs) {
        this(tenantId, productId, productVersion, planVersion, mode, currency, effectiveFrom, effectiveTo,
                rateTableRef, featureContract, artifactRef, calculationModelRef, roundingRule, taxPolicyRefs,
                List.of(), List.of());
    }

    public CreatePricingPlanDraftCommand(
            String tenantId,
            String productId,
            String productVersion,
            String planVersion,
            PricingMode mode,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            RateTableRef rateTableRef,
            PricingFeatureContract featureContract,
            PricingRuleArtifactRef artifactRef,
            CalculationModelRef calculationModelRef,
            PricingRoundingRule roundingRule) {
        this(tenantId, productId, productVersion, planVersion, mode, currency, effectiveFrom, effectiveTo,
                rateTableRef, featureContract, artifactRef, calculationModelRef, roundingRule, List.of(), List.of(),
                List.of());
    }

    public CreatePricingPlanDraftCommand(
            String tenantId,
            String productId,
            String productVersion,
            String planVersion,
            PricingMode mode,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            RateTableRef rateTableRef,
            PricingFeatureContract featureContract,
            PricingRuleArtifactRef artifactRef,
            PricingRoundingRule roundingRule) {
        this(tenantId, productId, productVersion, planVersion, mode, currency, effectiveFrom, effectiveTo,
                rateTableRef, featureContract, artifactRef, null, roundingRule, List.of(), List.of(), List.of());
    }
}
