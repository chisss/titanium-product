package com.titanium.product.valueobject.pricing.premium;

import java.math.BigDecimal;
import java.util.List;

/**
 * 结构化调整计算结果。
 */
public record PremiumAdjustmentResult(
        BigDecimal standardPremium,
        BigDecimal totalPremium,
        List<PremiumAdjustment> adjustments) {

    public PremiumAdjustmentResult {
        adjustments = adjustments == null ? List.of() : List.copyOf(adjustments);
    }
}
