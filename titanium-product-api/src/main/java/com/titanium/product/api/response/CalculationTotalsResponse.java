package com.titanium.product.api.response;

import java.math.BigDecimal;

/**
 * 客户价格与内部成本分流后的费用汇总。
 */
public record CalculationTotalsResponse(
        BigDecimal premiumSubtotal,
        BigDecimal taxAndLevyTotal,
        BigDecimal customerPayable,
        BigDecimal internalCostTotal) {
}
