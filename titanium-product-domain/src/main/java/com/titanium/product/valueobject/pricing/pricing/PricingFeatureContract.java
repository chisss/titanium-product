package com.titanium.product.valueobject.pricing.pricing;

import java.util.List;

/**
 * Product 定价方案持有的不可变特征契约。
 */
public record PricingFeatureContract(
        String contractId,
        String contractVersion,
        List<PricingFeatureRequirement> requirements) {

    public PricingFeatureContract {
        if (contractId == null || contractId.isBlank()) {
            throw new IllegalArgumentException("contractId不能为空");
        }
        if (contractVersion == null || contractVersion.isBlank()) {
            throw new IllegalArgumentException("contractVersion不能为空");
        }
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
        if (requirements.size() > 100) {
            throw new IllegalArgumentException("特征需求不能超过100项");
        }
    }
}
