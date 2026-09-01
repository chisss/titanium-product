package com.titanium.product.valueobject.pricing.pricing;

import java.util.Objects;

import com.titanium.product.common.enums.PricingFeatureDataType;

/**
 * Product 定价契约中的单项特征需求。
 */
public record PricingFeatureRequirement(
        String featureCode,
        PricingFeatureDataType dataType,
        boolean required,
        String definitionVersion,
        String missingPolicy,
        String sensitivity) {

    public PricingFeatureRequirement {
        if (featureCode == null || featureCode.isBlank()) {
            throw new IllegalArgumentException("featureCode不能为空");
        }
        Objects.requireNonNull(dataType, "dataType不能为空");
    }
}
