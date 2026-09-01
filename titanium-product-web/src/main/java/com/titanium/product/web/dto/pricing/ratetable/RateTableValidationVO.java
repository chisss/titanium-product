package com.titanium.product.web.dto.pricing.ratetable;

/** 费率表发布前校验响应。 */
public record RateTableValidationVO(boolean valid, long rowCount, String contentHash) {
}
