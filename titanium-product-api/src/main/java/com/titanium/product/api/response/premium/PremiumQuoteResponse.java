package com.titanium.product.api.response.premium;
import java.math.BigDecimal;
import java.util.List;

import com.titanium.product.api.response.calculation.CalculationLineResponse;
import com.titanium.product.api.response.calculation.CalculationTotalsResponse;

/**
 * Product 保费试算响应。
 */
public record PremiumQuoteResponse(
        String quoteId,
        String requestId,
        String productId,
        String productVersion,
        String currency,
        BigDecimal totalPremium,
        BigDecimal installmentAmount,
        int periods,
        BigDecimal matchedRate,
        String matchedRowId,
        String rateTableCode,
        String rateTableVersion,
        String rateTableContentHash,
        String pricingPlanVersion,
        String pricingPlanContentHash,
        String featureSnapshotId,
        String ruleArtifactCode,
        String ruleArtifactVersion,
        String ruleArtifactHash,
        int roundingScale,
        String roundingMode,
        String inputHash,
        String resultHash,
        CalculationTotalsResponse calculationTotals,
        List<CalculationLineResponse> calculationLines,
        String calculationModelCode,
        String calculationModelVersion,
        String calculationModelHash) {

    /**
     * 兼容 V1 调用方的旧构造签名。
     */
    public PremiumQuoteResponse(
            String quoteId,
            String requestId,
            String productId,
            String productVersion,
            String currency,
            BigDecimal totalPremium,
            BigDecimal installmentAmount,
            int periods,
            BigDecimal matchedRate,
            String matchedRowId,
            String rateTableCode,
            String rateTableVersion,
            String rateTableContentHash,
            String pricingPlanVersion,
            String pricingPlanContentHash,
            String featureSnapshotId,
            String ruleArtifactCode,
            String ruleArtifactVersion,
            String ruleArtifactHash,
            int roundingScale,
            String roundingMode,
            String inputHash,
            String resultHash) {
        this(quoteId, requestId, productId, productVersion, currency, totalPremium, installmentAmount, periods,
                matchedRate, matchedRowId, rateTableCode, rateTableVersion, rateTableContentHash,
                pricingPlanVersion, pricingPlanContentHash, featureSnapshotId, ruleArtifactCode,
                ruleArtifactVersion, ruleArtifactHash, roundingScale, roundingMode, inputHash, resultHash,
                new CalculationTotalsResponse(totalPremium, BigDecimal.ZERO, totalPremium, BigDecimal.ZERO),
                List.of(), null, null, null);
    }
}
