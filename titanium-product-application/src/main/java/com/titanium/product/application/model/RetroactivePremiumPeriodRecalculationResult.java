package com.titanium.product.application.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.titanium.product.common.enums.PremiumBalanceDirection;
import com.titanium.product.valueobject.pricing.premium.RetroactivePremiumPeriodDifference;

/** Product 追溯期间重算应用结果。 */
public record RetroactivePremiumPeriodRecalculationResult(
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
        PremiumBalanceDirection direction,
        BigDecimal amount,
        String currency,
        String inputHash,
        String resultHash,
        LocalDateTime calculatedAt,
        List<RetroactivePremiumPeriodDifference> periods) {

    public RetroactivePremiumPeriodRecalculationResult {
        periods = List.copyOf(periods);
    }
}
