package com.titanium.product.web.dto.pricing.dynamicfactor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 定价包引用的动态因子固定版本。 */
public record DynamicFactorRefDTO(
        @NotBlank String factorCode,
        @NotBlank String factorVersion,
        @NotBlank @Pattern(regexp = "[0-9a-fA-F]{64}") String contentHash) {
}
