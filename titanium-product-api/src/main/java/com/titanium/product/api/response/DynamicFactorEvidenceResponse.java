package com.titanium.product.api.response;

/** 确认计算使用的动态因子证据。 */
public record DynamicFactorEvidenceResponse(
        String factorCode,
        String factorVersion,
        String contentHash,
        String featureCode,
        String featureDefinitionVersion) {
}
