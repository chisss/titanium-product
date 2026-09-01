package com.titanium.product.valueobject.pricing.pricing;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Product 发送给 Rule Engine 的固定版本计算请求。
 */
public record PricingRuleComputationRequest(
        String tenantId,
        String executionId,
        String artifactCode,
        String artifactVersion,
        String inputSchemaVersion,
        Map<String, Object> variables,
        boolean explain,
        LocalDateTime businessTime) {

    public PricingRuleComputationRequest {
        requireText(tenantId, "tenantId");
        requireText(executionId, "executionId");
        requireText(artifactCode, "artifactCode");
        requireText(artifactVersion, "artifactVersion");
        requireText(inputSchemaVersion, "inputSchemaVersion");
        Objects.requireNonNull(businessTime, "businessTime不能为空");
        Objects.requireNonNull(variables, "variables不能为空");
        if (variables.size() > 200) {
            throw new IllegalArgumentException("variables数量不能超过200");
        }
        variables = Collections.unmodifiableMap(new LinkedHashMap<>(variables));
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "不能为空");
        }
    }
}
