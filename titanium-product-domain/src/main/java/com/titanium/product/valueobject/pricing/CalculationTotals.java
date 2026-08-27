package com.titanium.product.valueobject.pricing;

import java.math.BigDecimal;

/**
 * 客户价格与内部成本分流后的计算总计。
 */
public record CalculationTotals(
        BigDecimal premiumSubtotal,
        BigDecimal taxAndLevyTotal,
        BigDecimal customerPayable,
        BigDecimal internalCostTotal) {

    public CalculationTotals {
        if (premiumSubtotal == null || taxAndLevyTotal == null || customerPayable == null
                || internalCostTotal == null || premiumSubtotal.signum() < 0 || taxAndLevyTotal.signum() < 0
                || customerPayable.signum() < 0 || internalCostTotal.signum() < 0
                || premiumSubtotal.add(taxAndLevyTotal).compareTo(customerPayable) != 0) {
            throw new IllegalArgumentException("费用总计不守恒或包含负数");
        }
    }

    public static CalculationTotals customerPremium(BigDecimal amount) {
        return new CalculationTotals(amount, BigDecimal.ZERO, amount, BigDecimal.ZERO);
    }
}
