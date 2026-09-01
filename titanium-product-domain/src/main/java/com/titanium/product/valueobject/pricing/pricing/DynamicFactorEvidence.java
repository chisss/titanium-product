package com.titanium.product.valueobject.pricing.pricing;

/** 确认计算使用的动态因子版本证据。 */
public record DynamicFactorEvidence(
        String factorCode,
        String factorVersion,
        String contentHash,
        String featureCode,
        String featureDefinitionVersion) {
}
