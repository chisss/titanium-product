package com.titanium.product.valueobject.pricing.commission;

import java.math.BigDecimal;
import java.util.List;

/**
 * Channel 固定版本佣金计算结果。
 */
public record CommissionResolutionResult(
        String schemeCode,
        String schemeVersion,
        String schemeHash,
        String channelId,
        String productId,
        String currency,
        BigDecimal baseAmount,
        BigDecimal grossCommission,
        List<CommissionResolutionInstruction> instructions) {

    public CommissionResolutionResult {
        instructions = instructions == null ? List.of() : List.copyOf(instructions);
    }
}
