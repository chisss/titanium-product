package com.titanium.product.valueobject.pricing;

import java.math.BigDecimal;

/**
 * Channel 返回的单一受益方预计佣金指令。
 */
public record CommissionResolutionInstruction(
        String beneficiaryType,
        String beneficiaryId,
        BigDecimal splitRate,
        BigDecimal amount,
        int installmentCount,
        int clawbackMonths) {
}
