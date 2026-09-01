package com.titanium.product.api.response.premium;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Product 已持久化的保全版本化报价事实。 */
public record MaintenancePremiumQuoteResponse(
        String tenantId,
        String maintenanceId,
        String policyId,
        Long policyBaselineVersion,
        String productId,
        String productVersion,
        String planVersion,
        String itemCode,
        String beforeSnapshotHash,
        String proposedSnapshotHash,
        String quoteId,
        String quoteVersion,
        String originalCalculationId,
        String originalResultHash,
        String replacementCalculationId,
        String replacementResultHash,
        String pricingPlanVersion,
        String pricingPlanContentHash,
        String idempotencyKey,
        String payloadHash,
        String resultHash,
        String detailSummary,
        String direction,
        BigDecimal amount,
        String currency,
        LocalDateTime quotedAt,
        LocalDateTime validUntil) {
}
