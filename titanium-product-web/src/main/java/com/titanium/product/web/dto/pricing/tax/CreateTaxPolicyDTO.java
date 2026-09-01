package com.titanium.product.web.dto.pricing.tax;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 创建税费策略草稿请求。
 */
public record CreateTaxPolicyDTO(
        @NotBlank String policyCode,
        @NotBlank String policyVersion,
        @NotBlank String policyName,
        String description,
        @NotBlank String jurisdictionCode,
        @NotBlank String category,
        @NotBlank String payerType,
        @NotBlank String priceMode,
        @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal taxRate,
        @NotEmpty List<@NotBlank String> baseComponentCodes,
        @NotBlank String accountingClass,
        @NotBlank String regulatoryReferenceId,
        String exemptionFeatureCode,
        @NotNull LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo) {
}
