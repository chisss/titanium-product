package com.titanium.product.web.dto.pricing.surrender;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建退保价值策略草稿请求。
 */
public record CreateSurrenderValuePolicyDTO(
        @NotBlank String policyCode,
        @NotBlank String policyVersion,
        @Min(1) int policyYear,
        @Min(0) int coolingOffDays,
        @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal cashValueRate,
        @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal internalCostRetentionRate,
        @NotNull LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo) {
}
