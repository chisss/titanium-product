package com.titanium.product.command.maintenance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.titanium.product.common.enums.PremiumLifecycleType;
import com.titanium.product.valueobject.pricing.PremiumAdjustmentRequest;

/** 创建 Product 保全版本化报价的应用命令。 */
public record CreateMaintenancePremiumQuoteCommand(
        String tenantId,
        String productId,
        String maintenanceId,
        String policyId,
        long policyBaselineVersion,
        String itemCode,
        String productVersion,
        String planVersion,
        PremiumLifecycleType lifecycleType,
        SnapshotReference beforeSnapshot,
        SnapshotReference proposedSnapshot,
        String originalCalculationId,
        LocalDateTime businessTime,
        String currency,
        BigDecimal sumInsured,
        int age,
        String gender,
        int paymentTermYears,
        int coverageTermYears,
        int paymentPeriods,
        Map<String, Object> pricingFactors,
        List<PremiumAdjustmentRequest> underwritingAdjustments,
        String channelId,
        int policyYear,
        String reason,
        String idempotencyKey,
        String payloadHash) {

    public CreateMaintenancePremiumQuoteCommand {
        pricingFactors = pricingFactors == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(pricingFactors));
        underwritingAdjustments = underwritingAdjustments == null
                ? List.of()
                : List.copyOf(underwritingAdjustments);
    }

    /** 报价输入绑定的不可变保单快照引用。 */
    public record SnapshotReference(
            String storageKey,
            String contentHash,
            long policyVersion,
            OffsetDateTime capturedAt) {
    }
}
