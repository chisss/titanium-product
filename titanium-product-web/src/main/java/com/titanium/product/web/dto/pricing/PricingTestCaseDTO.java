package com.titanium.product.web.dto.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 定价测试用例请求。
 */
public record PricingTestCaseDTO(
        @NotBlank String caseCode,
        String description,
        @NotNull LocalDateTime businessTime,
        @NotNull BigDecimal sumInsured,
        int age,
        @NotBlank String gender,
        int paymentTermYears,
        int coverageTermYears,
        int paymentPeriods,
        Map<String, Object> requestSnapshot,
        @NotNull BigDecimal expectedPremium,
        BigDecimal tolerance) {
}
