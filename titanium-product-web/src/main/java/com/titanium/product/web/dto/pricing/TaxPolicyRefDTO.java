package com.titanium.product.web.dto.pricing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 定价包锁定的税费策略版本。
 */
public record TaxPolicyRefDTO(
        @NotBlank String policyCode,
        @NotBlank String policyVersion,
        @NotBlank @Pattern(regexp = "[0-9a-fA-F]{64}") String contentHash) {
}
