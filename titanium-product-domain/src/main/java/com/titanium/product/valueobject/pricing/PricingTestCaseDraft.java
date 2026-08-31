package com.titanium.product.valueobject.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 定价测试用例录入模型。
 */
public record PricingTestCaseDraft(
        String caseCode,
        String description,
        LocalDateTime businessTime,
        BigDecimal sumInsured,
        int age,
        String gender,
        int paymentTermYears,
        int coverageTermYears,
        int paymentPeriods,
        Map<String, Object> requestSnapshot,
        BigDecimal expectedPremium,
        BigDecimal tolerance) {
}
