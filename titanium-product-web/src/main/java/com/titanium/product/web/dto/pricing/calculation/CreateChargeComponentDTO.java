package com.titanium.product.web.dto.pricing.calculation;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建费用项草稿请求。
 */
public record CreateChargeComponentDTO(
        @NotBlank String componentCode,
        @NotBlank String componentVersion,
        @NotBlank String componentName,
        String description,
        @NotBlank String category,
        @NotBlank String amountChannel,
        @NotBlank String direction,
        @NotBlank String payerType,
        @NotBlank String calculationSource,
        @NotBlank String accountingClass,
        boolean customerVisible,
        @NotNull LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo) {
}
