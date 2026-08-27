package com.titanium.product.valueobject.pricing;

import java.util.List;

/**
 * 确认计算引用的全部版本证据。
 */
public record PremiumCalculationEvidence(
        String productVersion,
        String pricingPlanVersion,
        String pricingPlanContentHash,
        String rateTableCode,
        String rateTableVersion,
        String rateTableContentHash,
        String featureSnapshotId,
        String ruleArtifactCode,
        String ruleArtifactVersion,
        String ruleArtifactHash,
        int roundingScale,
        String roundingMode,
        String calculationModelCode,
        String calculationModelVersion,
        String calculationModelHash,
        List<DynamicFactorEvidence> dynamicFactorEvidence) {

    public PremiumCalculationEvidence {
        dynamicFactorEvidence = dynamicFactorEvidence == null ? List.of() : List.copyOf(dynamicFactorEvidence);
    }

    public PremiumCalculationEvidence(
            String productVersion,
            String pricingPlanVersion,
            String pricingPlanContentHash,
            String rateTableCode,
            String rateTableVersion,
            String rateTableContentHash,
            String featureSnapshotId,
            String ruleArtifactCode,
            String ruleArtifactVersion,
            String ruleArtifactHash,
            int roundingScale,
            String roundingMode,
            String calculationModelCode,
            String calculationModelVersion,
            String calculationModelHash) {
        this(productVersion, pricingPlanVersion, pricingPlanContentHash, rateTableCode, rateTableVersion,
                rateTableContentHash, featureSnapshotId, ruleArtifactCode, ruleArtifactVersion,
                ruleArtifactHash, roundingScale, roundingMode, calculationModelCode, calculationModelVersion,
                calculationModelHash, List.of());
    }

    public PremiumCalculationEvidence(
            String productVersion,
            String pricingPlanVersion,
            String pricingPlanContentHash,
            String rateTableCode,
            String rateTableVersion,
            String rateTableContentHash,
            String featureSnapshotId,
            String ruleArtifactCode,
            String ruleArtifactVersion,
            String ruleArtifactHash,
            int roundingScale,
            String roundingMode) {
        this(productVersion, pricingPlanVersion, pricingPlanContentHash, rateTableCode, rateTableVersion,
                rateTableContentHash, featureSnapshotId, ruleArtifactCode, ruleArtifactVersion,
                ruleArtifactHash, roundingScale, roundingMode, null, null, null, List.of());
    }
}
