package com.titanium.product.application.model.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Product 已持久化的保全版本化报价结果。 */
public record MaintenancePremiumQuoteResult(
        String tenantId,
        String maintenanceId,
        String policyId,
        long policyBaselineVersion,
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
