package com.titanium.product.command.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.valueobject.pricing.PremiumAdjustmentRequest;

/**
 * Product 确认计算应用命令。
 */
public record PremiumCalculationCommand(
        String tenantId,
        String productId,
        String calculationRequestId,
        String bizNo,
        PricingCalculationPurpose purpose,
        String productVersion,
        String expectedPricingPlanVersion,
        LocalDateTime businessTime,
        String currency,
        BigDecimal sumInsured,
        int age,
        String gender,
        int paymentTermYears,
        int coverageTermYears,
        int paymentPeriods,
        Map<String, Object> requestSnapshot,
        List<PremiumAdjustmentRequest> underwritingAdjustments,
        String channelId,
        int policyYear) {

    public PremiumCalculationCommand {
        requestSnapshot = requestSnapshot == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(requestSnapshot));
        underwritingAdjustments = underwritingAdjustments == null
                ? List.of()
                : List.copyOf(underwritingAdjustments);
        policyYear = policyYear <= 0 ? 1 : policyYear;
    }

    public PremiumCalculationCommand(
            String tenantId,
            String productId,
            String calculationRequestId,
            String bizNo,
            PricingCalculationPurpose purpose,
            String productVersion,
            LocalDateTime businessTime,
            String currency,
            BigDecimal sumInsured,
            int age,
            String gender,
            int paymentTermYears,
            int coverageTermYears,
            int paymentPeriods,
            Map<String, Object> requestSnapshot,
            List<PremiumAdjustmentRequest> underwritingAdjustments,
            String channelId,
            int policyYear) {
        this(tenantId, productId, calculationRequestId, bizNo, purpose, productVersion, null,
                businessTime, currency, sumInsured, age, gender, paymentTermYears, coverageTermYears,
                paymentPeriods, requestSnapshot, underwritingAdjustments, channelId, policyYear);
    }

    public PremiumCalculationCommand(
            String tenantId,
            String productId,
            String calculationRequestId,
            String bizNo,
            PricingCalculationPurpose purpose,
            String productVersion,
            LocalDateTime businessTime,
            String currency,
            BigDecimal sumInsured,
            int age,
            String gender,
            int paymentTermYears,
            int coverageTermYears,
            int paymentPeriods,
            Map<String, Object> requestSnapshot,
            List<PremiumAdjustmentRequest> underwritingAdjustments) {
        this(tenantId, productId, calculationRequestId, bizNo, purpose, productVersion, null, businessTime,
                currency, sumInsured, age, gender, paymentTermYears, coverageTermYears, paymentPeriods,
                requestSnapshot, underwritingAdjustments, null, 1);
    }
}
