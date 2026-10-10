package com.titanium.product.web.dto.pricing.paidup;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建减额缴清净费率策略草稿请求（g04-05）。
 * <p>
 * 🔴 费率<b>只设下界不设上界</b>：该值是「每 1 元保额的趸缴净保费」而非比例，
 * 高龄/高保单年度下大于 1 属正常业务事实，误加 {@code @DecimalMax("1")} 会拒绝合法配置。
 * </p>
 */
public record CreateReducedPaidUpPolicyDTO(
        @NotBlank String policyCode,
        @NotBlank String policyVersion,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal netSinglePremiumRatePerUnit,
        @NotNull LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo) {
}
