package com.titanium.product.valueobject.pricing.pricing;

/**
 * Product 定价方案引用的固定规则工件。
 */
public record PricingRuleArtifactRef(
        String artifactCode,
        String artifactVersion,
        String inputSchemaVersion,
        String artifactHash) {

    public PricingRuleArtifactRef {
        requireText(artifactCode, "artifactCode");
        requireText(artifactVersion, "artifactVersion");
        requireText(inputSchemaVersion, "inputSchemaVersion");
        requireText(artifactHash, "artifactHash");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "不能为空");
        }
    }
}
