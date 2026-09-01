package com.titanium.product.valueobject.pricing.pricing;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Feature Center 返回的可重放特征解析快照。
 */
public record PricingFeatureResolution(
        String snapshotId,
        List<PricingFeatureValue> values,
        Map<String, String> definitionVersions,
        List<String> missingRequired,
        String lineageDigest) {

    public PricingFeatureResolution {
        requireText(snapshotId, "snapshotId");
        values = List.copyOf(Objects.requireNonNull(values, "values不能为空"));
        definitionVersions = definitionVersions == null ? Map.of() : Map.copyOf(definitionVersions);
        missingRequired = missingRequired == null ? List.of() : List.copyOf(missingRequired);
        requireText(lineageDigest, "lineageDigest");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "不能为空");
        }
    }
}
