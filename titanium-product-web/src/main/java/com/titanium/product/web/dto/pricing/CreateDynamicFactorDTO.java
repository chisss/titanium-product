package com.titanium.product.web.dto.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 创建动态因子草稿请求。 */
public record CreateDynamicFactorDTO(
        @NotBlank String factorCode,
        @NotBlank String factorVersion,
        @NotBlank String factorName,
        String description,
        @NotBlank String featureCode,
        @NotBlank String featureDefinitionVersion,
        @NotBlank String sourceType,
        @NotBlank String valueTimePolicy,
        BigDecimal lowerBound,
        BigDecimal upperBound,
        @NotBlank String missingPolicy,
        BigDecimal defaultValue,
        @NotBlank String transformType,
        @NotNull BigDecimal multiplier,
        @NotNull BigDecimal offset,
        boolean replayable,
        @NotNull LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo) {
}
