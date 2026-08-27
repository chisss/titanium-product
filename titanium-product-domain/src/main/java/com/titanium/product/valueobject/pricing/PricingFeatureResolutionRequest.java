package com.titanium.product.valueobject.pricing;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Product 发送给 Feature Center 的不可变特征契约快照。
 */
public record PricingFeatureResolutionRequest(
        String tenantId,
        String requestId,
        String contractId,
        String contractVersion,
        LocalDateTime businessTime,
        List<PricingFeatureRequirement> requirements,
        Map<String, Object> requestSnapshot,
        Map<String, String> sourceReferences) {

    public PricingFeatureResolutionRequest {
        requireText(tenantId, "tenantId");
        requireText(requestId, "requestId");
        requireText(contractId, "contractId");
        requireText(contractVersion, "contractVersion");
        Objects.requireNonNull(businessTime, "businessTime不能为空");
        requirements = List.copyOf(Objects.requireNonNull(requirements, "requirements不能为空"));
        if (requirements.isEmpty() || requirements.size() > 100) {
            throw new IllegalArgumentException("requirements数量必须在1到100之间");
        }
        requestSnapshot = immutableMap(Objects.requireNonNull(requestSnapshot, "requestSnapshot不能为空"));
        sourceReferences = immutableMap(sourceReferences);
    }

    private static <T> Map<String, T> immutableMap(Map<String, T> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "不能为空");
        }
    }
}
