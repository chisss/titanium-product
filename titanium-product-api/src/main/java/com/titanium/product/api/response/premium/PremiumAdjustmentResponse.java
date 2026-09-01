package com.titanium.product.api.response.premium;

import java.math.BigDecimal;

/**
 * 已固化的保费调整项响应。
 */
public record PremiumAdjustmentResponse(
        String adjustmentCode,
        String type,
        BigDecimal value,
        BigDecimal adjustmentAmount,
        BigDecimal premiumAfter,
        String reason,
        String ruleVersion) {
}
