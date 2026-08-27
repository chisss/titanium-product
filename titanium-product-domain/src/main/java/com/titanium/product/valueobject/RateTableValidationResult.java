package com.titanium.product.valueobject;

/**
 * 费率表发布前校验结果。
 *
 * @param rowCount 费率行数
 * @param contentHash 规范化内容哈希
 */
public record RateTableValidationResult(long rowCount, String contentHash) {
}
