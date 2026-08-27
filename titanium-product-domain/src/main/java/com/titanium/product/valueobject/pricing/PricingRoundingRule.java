package com.titanium.product.valueobject.pricing;

import java.math.RoundingMode;
import java.util.Objects;

/**
 * 定价方案最终金额舍入规则。
 */
public record PricingRoundingRule(int scale, RoundingMode roundingMode) {

    public PricingRoundingRule {
        if (scale < 0 || scale > 8) {
            throw new IllegalArgumentException("金额精度必须在0到8之间");
        }
        Objects.requireNonNull(roundingMode, "roundingMode不能为空");
    }
}
