package com.titanium.product.application.orchestration.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PricingPlan 单次计算输入。
 */
public record PricingCalculationInput(
        String tenantId,
        String productId,
        String executionId,
        LocalDateTime businessTime,
        String currency,
        BigDecimal sumInsured,
        int age,
        String gender,
        int paymentTermYears,
        int coverageTermYears,
        int paymentPeriods,
        Map<String, Object> requestSnapshot,
        String channelId,
        int policyYear) {

    public PricingCalculationInput {
        requestSnapshot = requestSnapshot == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(requestSnapshot));
        policyYear = policyYear <= 0 ? 1 : policyYear;
    }

    public PricingCalculationInput(
            String tenantId,
            String productId,
            String executionId,
            LocalDateTime businessTime,
            String currency,
            BigDecimal sumInsured,
            int age,
            String gender,
            int paymentTermYears,
            int coverageTermYears,
            int paymentPeriods,
            Map<String, Object> requestSnapshot) {
        this(tenantId, productId, executionId, businessTime, currency, sumInsured, age, gender,
                paymentTermYears, coverageTermYears, paymentPeriods, requestSnapshot, null, 1);
    }
}
