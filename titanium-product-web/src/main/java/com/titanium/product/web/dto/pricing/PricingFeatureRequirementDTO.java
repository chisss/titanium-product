package com.titanium.product.web.dto.pricing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 特征需求请求。
 */
public record PricingFeatureRequirementDTO(
        @NotBlank String featureCode,
        @NotBlank String dataType,
        @NotNull Boolean required,
        String definitionVersion,
        String missingPolicy,
        String sensitivity) {
}
