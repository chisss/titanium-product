package com.titanium.product.valueobject.pricing.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 定价方案发布回归用例。
 */
public record PricingTestCase(
        String caseId,
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

    public PricingTestCase {
        requireText(caseId, "caseId");
        requireText(caseCode, "caseCode");
        Objects.requireNonNull(businessTime, "businessTime不能为空");
        if (sumInsured == null || sumInsured.signum() <= 0 || age < 0 || age > 120
                || paymentTermYears <= 0 || coverageTermYears <= 0 || paymentPeriods <= 0) {
            throw new IllegalArgumentException("测试用例标准输入不合法");
        }
        requireText(gender, "gender");
        requestSnapshot = requestSnapshot == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(requestSnapshot));
        if (expectedPremium == null || expectedPremium.signum() < 0) {
            throw new IllegalArgumentException("expectedPremium不能为负数");
        }
        tolerance = tolerance == null ? BigDecimal.ZERO : tolerance;
        if (tolerance.signum() < 0) {
            throw new IllegalArgumentException("tolerance不能为负数");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "不能为空");
        }
    }
}
