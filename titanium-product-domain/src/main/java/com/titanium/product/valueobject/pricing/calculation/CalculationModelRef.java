package com.titanium.product.valueobject.pricing.calculation;

/**
 * 定价包引用的不可变计算模型版本。
 */
public record CalculationModelRef(String modelCode, String modelVersion, String contentHash) {

    public CalculationModelRef {
        if (modelCode == null || modelCode.isBlank() || modelVersion == null || modelVersion.isBlank()
                || contentHash == null || contentHash.length() != 64) {
            throw new IllegalArgumentException("计算模型编码、版本和SHA-256不能为空");
        }
    }
}
