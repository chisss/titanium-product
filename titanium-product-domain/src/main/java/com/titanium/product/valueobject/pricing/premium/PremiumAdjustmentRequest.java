package com.titanium.product.valueobject.pricing.premium;

import java.math.BigDecimal;

import com.titanium.product.common.enums.PremiumAdjustmentType;

/**
 * 确认计算待应用的结构化调整项。
 */
public record PremiumAdjustmentRequest(
        String adjustmentCode,
        PremiumAdjustmentType type,
        BigDecimal value,
        String reason,
        String ruleVersion) {
}
