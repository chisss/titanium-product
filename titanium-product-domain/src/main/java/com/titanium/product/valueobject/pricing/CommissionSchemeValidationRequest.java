package com.titanium.product.valueobject.pricing;

import java.time.LocalDateTime;

/**
 * Product 创建定价包时校验 Channel 精确引用的请求。
 */
public record CommissionSchemeValidationRequest(
        String tenantId,
        String productId,
        String currency,
        LocalDateTime businessTime,
        CommissionSchemeRef reference) {
}
