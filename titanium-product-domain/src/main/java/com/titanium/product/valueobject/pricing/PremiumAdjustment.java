package com.titanium.product.valueobject.pricing;

import java.math.BigDecimal;

import com.titanium.product.common.enums.PremiumAdjustmentType;

/**
 * 已应用并固化金额的保费调整项。
 */
public record PremiumAdjustment(
        String adjustmentCode,
        PremiumAdjustmentType type,
        BigDecimal value,
        BigDecimal adjustmentAmount,
        BigDecimal premiumAfter,
        String reason,
        String ruleVersion) {
}
