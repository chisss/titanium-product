package com.titanium.product.api.response.premium;

import java.math.BigDecimal;

/**
 * 生命周期费用单项差额响应。
 */
public record PremiumLifecycleDifferenceLineResponse(
        String lineId,
        String componentCode,
        String originalComponentVersion,
        String replacementComponentVersion,
        String category,
        String amountChannel,
        String direction,
        String payerType,
        String accountingClass,
        String currency,
        String originalDirection,
        BigDecimal beforeAmount,
        String replacementDirection,
        BigDecimal afterAmount,
        BigDecimal differenceAmount,
        boolean customerVisible,
        boolean affectsCustomerPayable,
        String description) {
}
