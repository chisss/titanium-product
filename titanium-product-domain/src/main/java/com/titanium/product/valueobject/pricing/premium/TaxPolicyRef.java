package com.titanium.product.valueobject.pricing.premium;

/**
 * 定价包引用的不可变税费策略版本。
 */
public record TaxPolicyRef(String policyCode, String policyVersion, String contentHash) {

    public TaxPolicyRef {
        if (policyCode == null || policyCode.isBlank() || policyVersion == null || policyVersion.isBlank()
                || contentHash == null || contentHash.length() != 64) {
            throw new IllegalArgumentException("税费策略编码、版本和SHA-256不能为空");
        }
    }
}
