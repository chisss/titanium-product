package com.titanium.product.valueobject.pricing.pricing;

/** 定价包锁定的动态因子版本证据。 */
public record DynamicFactorRef(String factorCode, String factorVersion, String contentHash) {

    public DynamicFactorRef {
        requireText(factorCode, "factorCode");
        requireText(factorVersion, "factorVersion");
        if (contentHash == null || !contentHash.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException("contentHash必须为SHA-256");
        }
        contentHash = contentHash.toLowerCase();
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "不能为空");
        }
    }
}
