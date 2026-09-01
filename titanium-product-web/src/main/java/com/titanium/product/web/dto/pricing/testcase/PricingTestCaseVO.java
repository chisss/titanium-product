package com.titanium.product.web.dto.pricing.testcase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 定价测试用例后台响应。
 */
public record PricingTestCaseVO(
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
}
