package com.titanium.product.api.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Product 确认计算响应。
 */
public record PremiumCalculationResponse(
        String calculationId,
        String calculationRequestId,
        String bizNo,
        String purpose,
        String status,
        String productId,
        String productVersion,
        String currency,
        BigDecimal standardPremium,
        BigDecimal totalPremium,
        BigDecimal installmentAmount,
        int periods,
        List<PremiumAdjustmentResponse> adjustments,
        String pricingPlanVersion,
        String pricingPlanContentHash,
        String rateTableCode,
        String rateTableVersion,
        String rateTableContentHash,
        String featureSnapshotId,
        String ruleArtifactCode,
        String ruleArtifactVersion,
        String ruleArtifactHash,
        String requestHash,
        String inputHash,
        String resultHash,
        LocalDateTime createdAt,
        CalculationTotalsResponse calculationTotals,
        List<CalculationLineResponse> calculationLines,
        String calculationModelCode,
        String calculationModelVersion,
        String calculationModelHash,
        List<DynamicFactorEvidenceResponse> dynamicFactorEvidence) {

    public PremiumCalculationResponse(
            String calculationId,
            String calculationRequestId,
            String bizNo,
            String purpose,
            String status,
            String productId,
            String productVersion,
            String currency,
            BigDecimal standardPremium,
            BigDecimal totalPremium,
            BigDecimal installmentAmount,
            int periods,
            List<PremiumAdjustmentResponse> adjustments,
            String pricingPlanVersion,
            String pricingPlanContentHash,
            String rateTableCode,
            String rateTableVersion,
            String rateTableContentHash,
            String featureSnapshotId,
            String ruleArtifactCode,
            String ruleArtifactVersion,
            String ruleArtifactHash,
            String requestHash,
            String inputHash,
            String resultHash,
            LocalDateTime createdAt,
            CalculationTotalsResponse calculationTotals,
            List<CalculationLineResponse> calculationLines,
            String calculationModelCode,
            String calculationModelVersion,
            String calculationModelHash) {
        this(calculationId, calculationRequestId, bizNo, purpose, status, productId, productVersion, currency,
                standardPremium, totalPremium, installmentAmount, periods, adjustments, pricingPlanVersion,
                pricingPlanContentHash, rateTableCode, rateTableVersion, rateTableContentHash, featureSnapshotId,
                ruleArtifactCode, ruleArtifactVersion, ruleArtifactHash, requestHash, inputHash, resultHash,
                createdAt, calculationTotals, calculationLines, calculationModelCode, calculationModelVersion,
                calculationModelHash, List.of());
    }

    /**
     * 兼容 V1 调用方的旧构造签名。
     */
    public PremiumCalculationResponse(
            String calculationId,
            String calculationRequestId,
            String bizNo,
            String purpose,
            String status,
            String productId,
            String productVersion,
            String currency,
            BigDecimal standardPremium,
            BigDecimal totalPremium,
            BigDecimal installmentAmount,
            int periods,
            List<PremiumAdjustmentResponse> adjustments,
            String pricingPlanVersion,
            String pricingPlanContentHash,
            String rateTableCode,
            String rateTableVersion,
            String rateTableContentHash,
            String featureSnapshotId,
            String ruleArtifactCode,
            String ruleArtifactVersion,
            String ruleArtifactHash,
            String requestHash,
            String inputHash,
            String resultHash,
            LocalDateTime createdAt) {
        this(calculationId, calculationRequestId, bizNo, purpose, status, productId, productVersion, currency,
                standardPremium, totalPremium, installmentAmount, periods, adjustments, pricingPlanVersion,
                pricingPlanContentHash, rateTableCode, rateTableVersion, rateTableContentHash, featureSnapshotId,
                ruleArtifactCode, ruleArtifactVersion, ruleArtifactHash, requestHash, inputHash, resultHash,
                createdAt,
                new CalculationTotalsResponse(totalPremium, BigDecimal.ZERO, totalPremium, BigDecimal.ZERO),
                List.of(), null, null, null, List.of());
    }
}
