package com.titanium.product.command.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Product 保费试算应用命令。
 */
public record PremiumQuoteCommand(
        String tenantId,
        String productId,
        String requestId,
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

    public PremiumQuoteCommand {
        requestSnapshot = requestSnapshot == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(requestSnapshot));
        policyYear = policyYear <= 0 ? 1 : policyYear;
    }

    public PremiumQuoteCommand(
            String tenantId,
            String productId,
            String requestId,
            LocalDateTime businessTime,
            String currency,
            BigDecimal sumInsured,
            int age,
            String gender,
            int paymentTermYears,
            int coverageTermYears,
            int paymentPeriods,
            Map<String, Object> requestSnapshot) {
        this(tenantId, productId, requestId, businessTime, currency, sumInsured, age, gender,
                paymentTermYears, coverageTermYears, paymentPeriods, requestSnapshot, null, 1);
    }

    public PremiumQuoteCommand(
            String tenantId,
            String productId,
            String requestId,
            LocalDateTime businessTime,
            String currency,
            BigDecimal sumInsured,
            int age,
            String gender,
            int paymentTermYears,
            int coverageTermYears,
            int paymentPeriods) {
        this(tenantId, productId, requestId, businessTime, currency, sumInsured, age, gender,
                paymentTermYears, coverageTermYears, paymentPeriods, Map.of(), null, 1);
    }
}
