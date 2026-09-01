package com.titanium.product.web.dto.pricing.factor;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * 特征契约请求。
 */
public record PricingFeatureContractDTO(
        @NotBlank String contractId,
        @NotBlank String contractVersion,
        @Valid List<PricingFeatureRequirementDTO> requirements) {
}
