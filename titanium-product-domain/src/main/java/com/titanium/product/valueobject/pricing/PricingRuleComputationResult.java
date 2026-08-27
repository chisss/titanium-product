package com.titanium.product.valueobject.pricing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Rule Engine 固定版本计算结果及审计证据。
 */
public record PricingRuleComputationResult(
        String executionId,
        String artifactCode,
        String artifactVersion,
        String inputSchemaVersion,
        BigDecimal computedValue,
        Map<String, BigDecimal> lineItems,
        List<String> matchedSteps,
        String artifactHash,
        long durationMs) {

    public PricingRuleComputationResult {
        requireText(executionId, "executionId");
        requireText(artifactCode, "artifactCode");
        requireText(artifactVersion, "artifactVersion");
        requireText(inputSchemaVersion, "inputSchemaVersion");
        Objects.requireNonNull(computedValue, "computedValue不能为空");
        lineItems = lineItems == null ? Map.of() : Map.copyOf(lineItems);
        matchedSteps = matchedSteps == null ? List.of() : List.copyOf(matchedSteps);
        requireText(artifactHash, "artifactHash");
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs不能为负数");
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "不能为空");
        }
    }
}
