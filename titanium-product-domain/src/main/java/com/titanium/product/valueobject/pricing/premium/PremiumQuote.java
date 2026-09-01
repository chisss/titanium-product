package com.titanium.product.valueobject.pricing.premium;

import java.math.BigDecimal;
import java.util.List;

import com.titanium.product.valueobject.pricing.calculation.CalculationLine;
import com.titanium.product.valueobject.pricing.calculation.CalculationTotals;
import com.titanium.product.valueobject.pricing.pricing.DynamicFactorEvidence;

/**
 * Product 保费试算结果及可重放版本证据。
 */
public record PremiumQuote(
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
        CalculationTotals calculationTotals,
        List<CalculationLine> calculationLines,
        String calculationModelCode,
        String calculationModelVersion,
        String calculationModelHash,
        List<DynamicFactorEvidence> dynamicFactorEvidence) {

    public PremiumQuote {
        calculationLines = calculationLines == null ? List.of() : List.copyOf(calculationLines);
        dynamicFactorEvidence = dynamicFactorEvidence == null ? List.of() : List.copyOf(dynamicFactorEvidence);
    }

    public PremiumQuote(
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
            CalculationTotals calculationTotals,
            List<CalculationLine> calculationLines,
            String calculationModelCode,
            String calculationModelVersion,
            String calculationModelHash) {
        this(quoteId, requestId, productId, productVersion, currency, totalPremium, installmentAmount, periods,
                matchedRate, matchedRowId, rateTableCode, rateTableVersion, rateTableContentHash,
                pricingPlanVersion, pricingPlanContentHash, featureSnapshotId, ruleArtifactCode,
                ruleArtifactVersion, ruleArtifactHash, roundingScale, roundingMode, inputHash, resultHash,
                calculationTotals, calculationLines, calculationModelCode, calculationModelVersion,
                calculationModelHash, List.of());
    }

    public PremiumQuote(
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
                CalculationTotals.customerPremium(totalPremium), List.of(), null, null, null, List.of());
    }
}
