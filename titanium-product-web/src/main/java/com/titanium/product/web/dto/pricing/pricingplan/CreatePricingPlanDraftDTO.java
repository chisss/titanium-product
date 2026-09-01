package com.titanium.product.web.dto.pricing.pricingplan;
import java.time.LocalDateTime;
import java.util.List;

import com.titanium.product.web.dto.pricing.commission.CommissionSchemeRefDTO;
import com.titanium.product.web.dto.pricing.dynamicfactor.DynamicFactorRefDTO;
import com.titanium.product.web.dto.pricing.factor.PricingFeatureContractDTO;
import com.titanium.product.web.dto.pricing.ruleartifact.PricingRuleArtifactRefDTO;
import com.titanium.product.web.dto.pricing.tax.TaxPolicyRefDTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 定价方案草稿请求。
 */
public record CreatePricingPlanDraftDTO(
        @NotBlank String productVersion,
        @NotBlank String planVersion,
        @NotBlank String pricingMode,
        @NotBlank String currency,
        @NotNull LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        String rateTableCode,
        String rateTableVersion,
        List<String> rateDimensionKeys,
        @Valid PricingFeatureContractDTO featureContract,
        @Valid PricingRuleArtifactRefDTO artifactRef,
        String calculationModelCode,
        String calculationModelVersion,
        String calculationModelHash,
        @NotNull Integer roundingScale,
        @NotBlank String roundingMode,
        @Valid List<TaxPolicyRefDTO> taxPolicyRefs,
        @Valid List<CommissionSchemeRefDTO> commissionSchemeRefs,
        @Valid List<DynamicFactorRefDTO> dynamicFactorRefs) {

    public CreatePricingPlanDraftDTO(
            String productVersion,
            String planVersion,
            String pricingMode,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String rateTableCode,
            String rateTableVersion,
            List<String> rateDimensionKeys,
            PricingFeatureContractDTO featureContract,
            PricingRuleArtifactRefDTO artifactRef,
            String calculationModelCode,
            String calculationModelVersion,
            String calculationModelHash,
            Integer roundingScale,
            String roundingMode,
            List<TaxPolicyRefDTO> taxPolicyRefs,
            List<CommissionSchemeRefDTO> commissionSchemeRefs) {
        this(productVersion, planVersion, pricingMode, currency, effectiveFrom, effectiveTo, rateTableCode,
                rateTableVersion, rateDimensionKeys, featureContract, artifactRef, calculationModelCode,
                calculationModelVersion, calculationModelHash, roundingScale, roundingMode, taxPolicyRefs,
                commissionSchemeRefs, List.of());
    }

    public CreatePricingPlanDraftDTO(
            String productVersion,
            String planVersion,
            String pricingMode,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String rateTableCode,
            String rateTableVersion,
            List<String> rateDimensionKeys,
            PricingFeatureContractDTO featureContract,
            PricingRuleArtifactRefDTO artifactRef,
            String calculationModelCode,
            String calculationModelVersion,
            String calculationModelHash,
            Integer roundingScale,
            String roundingMode) {
        this(productVersion, planVersion, pricingMode, currency, effectiveFrom, effectiveTo, rateTableCode,
                rateTableVersion, rateDimensionKeys, featureContract, artifactRef, calculationModelCode,
                calculationModelVersion, calculationModelHash, roundingScale, roundingMode, List.of(), List.of(),
                List.of());
    }

    public CreatePricingPlanDraftDTO(
            String productVersion,
            String planVersion,
            String pricingMode,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String rateTableCode,
            String rateTableVersion,
            List<String> rateDimensionKeys,
            PricingFeatureContractDTO featureContract,
            PricingRuleArtifactRefDTO artifactRef,
            Integer roundingScale,
            String roundingMode) {
        this(productVersion, planVersion, pricingMode, currency, effectiveFrom, effectiveTo, rateTableCode,
                rateTableVersion, rateDimensionKeys, featureContract, artifactRef, null, null, null,
                roundingScale, roundingMode, List.of(), List.of(), List.of());
    }

    public CreatePricingPlanDraftDTO(
            String productVersion,
            String planVersion,
            String pricingMode,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String rateTableCode,
            String rateTableVersion,
            List<String> rateDimensionKeys,
            PricingFeatureContractDTO featureContract,
            PricingRuleArtifactRefDTO artifactRef,
            String calculationModelCode,
            String calculationModelVersion,
            String calculationModelHash,
            Integer roundingScale,
            String roundingMode,
            List<TaxPolicyRefDTO> taxPolicyRefs) {
        this(productVersion, planVersion, pricingMode, currency, effectiveFrom, effectiveTo, rateTableCode,
                rateTableVersion, rateDimensionKeys, featureContract, artifactRef, calculationModelCode,
                calculationModelVersion, calculationModelHash, roundingScale, roundingMode, taxPolicyRefs,
                List.of(), List.of());
    }
}
