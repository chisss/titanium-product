package com.titanium.product.valueobject.pricing;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Product 调用 Channel 计算固定版本预计佣金的请求。
 */
public record CommissionResolutionRequest(
        String tenantId,
        String productId,
        String channelId,
        String currency,
        LocalDateTime businessTime,
        int policyYear,
        int paymentPeriods,
        int roundingScale,
        RoundingMode roundingMode,
        CommissionSchemeRef reference,
        List<CommissionBaseComponent> baseComponents) {

    public CommissionResolutionRequest {
        baseComponents = baseComponents == null ? List.of() : List.copyOf(baseComponents);
    }
}
