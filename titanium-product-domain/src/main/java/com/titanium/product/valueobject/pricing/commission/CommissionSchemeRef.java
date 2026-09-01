package com.titanium.product.valueobject.pricing.commission;

/**
 * 定价包对 Channel 佣金方案的精确版本引用。
 */
public record CommissionSchemeRef(
        String channelId,
        String schemeCode,
        String schemeVersion,
        String contentHash) {

    public CommissionSchemeRef {
        if (channelId == null || channelId.isBlank() || schemeCode == null || schemeCode.isBlank()
                || schemeVersion == null || schemeVersion.isBlank()
                || contentHash == null || contentHash.length() != 64) {
            throw new IllegalArgumentException("佣金方案引用必须包含渠道、编码、版本和64位hash");
        }
    }
}
