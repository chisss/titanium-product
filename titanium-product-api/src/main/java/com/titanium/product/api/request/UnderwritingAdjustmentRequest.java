package com.titanium.product.api.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

/**
 * 确认计算核保调整项请求。
 */
public record UnderwritingAdjustmentRequest(
        @NotBlank String adjustmentCode,
        @NotBlank String type,
        @DecimalMin(value = "0", inclusive = true) BigDecimal value,
        String reason,
        String ruleVersion) {
}
