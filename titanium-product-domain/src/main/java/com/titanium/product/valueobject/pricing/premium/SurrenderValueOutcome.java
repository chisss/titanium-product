package com.titanium.product.valueobject.pricing.premium;

import java.math.BigDecimal;

import com.titanium.product.common.enums.SurrenderRefundType;

/** 退保价值策略对一张保单产生的确定性结果。 */
public record SurrenderValueOutcome(
        SurrenderRefundType refundType,
        boolean withinCoolingOff,
        BigDecimal cashValueRate,
        BigDecimal refundAmount,
        BigDecimal retainedCustomerAmount,
        BigDecimal internalCostRetentionRate) {
}
