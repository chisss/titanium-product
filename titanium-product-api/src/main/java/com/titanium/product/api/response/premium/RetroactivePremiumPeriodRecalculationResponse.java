package com.titanium.product.api.response.premium;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Product 追溯期间重算权威结果。 */
public record RetroactivePremiumPeriodRecalculationResponse(
        String tenantId,
        String recalculationId,
        String recalculationVersion,
        String recalculationRequestId,
        String maintenanceId,
        String policyId,
        String analysisId,
        int analysisVersion,
        String analysisResultHash,
        String productId,
        String productVersion,
        String originalCalculationId,
        String originalResultHash,
        String replacementCalculationId,
        String replacementResultHash,
        LocalDateTime scopeFrom,
        LocalDateTime scopeTo,
        String direction,
        BigDecimal amount,
        String currency,
        String inputHash,
        String resultHash,
        LocalDateTime calculatedAt,
        List<PeriodDifferenceResponse> periods) {

    public RetroactivePremiumPeriodRecalculationResponse {
        periods = List.copyOf(periods);
    }

    /** 单个历史账务期间的重算前后金额和差额。 */
    public record PeriodDifferenceResponse(
            String periodId,
            String sourceReferenceId,
            LocalDateTime periodStart,
            BigDecimal originalAmount,
            BigDecimal recalculatedAmount,
            String direction,
            BigDecimal differenceAmount,
            String currency,
            String sourceEvidenceHash,
            String resultHash) {
    }
}
