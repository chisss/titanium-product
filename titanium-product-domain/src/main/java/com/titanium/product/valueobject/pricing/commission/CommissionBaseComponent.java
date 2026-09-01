package com.titanium.product.valueobject.pricing.commission;

import java.math.BigDecimal;

/**
 * 传递给 Channel 的佣金基数候选费用行。
 */
public record CommissionBaseComponent(String componentCode, BigDecimal amount) {
}
