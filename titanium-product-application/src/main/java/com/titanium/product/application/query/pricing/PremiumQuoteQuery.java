package com.titanium.product.application.query.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Product 保费试算查询。
 */
public record PremiumQuoteQuery(
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

    public PremiumQuoteQuery(
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
}
