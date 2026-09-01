package com.titanium.product.web.dto.pricing.ruleartifact;

import jakarta.validation.constraints.NotBlank;

/**
 * 规则工件引用请求。
 */
public record PricingRuleArtifactRefDTO(
        @NotBlank String artifactCode,
        @NotBlank String artifactVersion,
        @NotBlank String inputSchemaVersion,
        @NotBlank String artifactHash) {
}
