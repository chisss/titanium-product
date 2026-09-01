package com.titanium.product.application.orchestration.pricing;

import java.math.BigDecimal;
import java.util.List;

import com.titanium.product.valueobject.pricing.calculation.CalculationModelExecutionResult;
import com.titanium.product.valueobject.pricing.calculation.CalculationTotals;
import com.titanium.product.valueobject.pricing.pricing.DynamicFactorEvidence;

/**
 * PricingPlan 单次计算结果和版本证据。
 */
public record PricingCalculationOutcome(
        BigDecimal totalPremium,
        BigDecimal matchedRate,
        String matchedRateRowId,
        String rateTableCode,
        String rateTableVersion,
        String rateTableContentHash,
        String featureSnapshotId,
        String ruleArtifactCode,
        String ruleArtifactVersion,
        String ruleArtifactHash,
        CalculationModelExecutionResult breakdown,
        String calculationModelCode,
        String calculationModelVersion,
        String calculationModelHash,
        List<DynamicFactorEvidence> dynamicFactorEvidence) {

    public PricingCalculationOutcome {
        dynamicFactorEvidence = dynamicFactorEvidence == null ? List.of() : List.copyOf(dynamicFactorEvidence);
    }

    public PricingCalculationOutcome(
            BigDecimal totalPremium,
            BigDecimal matchedRate,
            String matchedRateRowId,
            String rateTableCode,
            String rateTableVersion,
            String rateTableContentHash,
            String featureSnapshotId,
            String ruleArtifactCode,
            String ruleArtifactVersion,
            String ruleArtifactHash,
            CalculationModelExecutionResult breakdown,
            String calculationModelCode,
            String calculationModelVersion,
            String calculationModelHash) {
        this(totalPremium, matchedRate, matchedRateRowId, rateTableCode, rateTableVersion, rateTableContentHash,
                featureSnapshotId, ruleArtifactCode, ruleArtifactVersion, ruleArtifactHash, breakdown,
                calculationModelCode, calculationModelVersion, calculationModelHash, List.of());
    }

    public PricingCalculationOutcome(
            BigDecimal totalPremium,
            BigDecimal matchedRate,
            String matchedRateRowId,
            String rateTableCode,
            String rateTableVersion,
            String rateTableContentHash,
            String featureSnapshotId,
            String ruleArtifactCode,
            String ruleArtifactVersion,
            String ruleArtifactHash) {
        this(totalPremium, matchedRate, matchedRateRowId, rateTableCode, rateTableVersion, rateTableContentHash,
                featureSnapshotId, ruleArtifactCode, ruleArtifactVersion, ruleArtifactHash,
                new CalculationModelExecutionResult(
                        java.util.List.of(), CalculationTotals.customerPremium(totalPremium), "LEGACY_TOTAL"),
                null, null, null, List.of());
    }
}
