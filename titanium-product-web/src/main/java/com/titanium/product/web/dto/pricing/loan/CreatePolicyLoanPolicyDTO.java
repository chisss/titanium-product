package com.titanium.product.web.dto.pricing.loan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建保单贷款策略草稿请求（g04-03）。
 */
public record CreatePolicyLoanPolicyDTO(
        @NotBlank String policyCode,
        @NotBlank String policyVersion,
        @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal loanableRatio,
        @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal interestRate,
        @NotNull LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo) {
}
