package com.titanium.product.service;

import java.math.BigDecimal;

import com.titanium.product.common.enums.PremiumBalanceDirection;

/** 追溯期间价格差异纯领域计算。 */
public class RetroactivePremiumPeriodDifferenceService {

    public Difference compare(BigDecimal originalAmount, BigDecimal recalculatedAmount) {
        if (originalAmount == null || originalAmount.signum() < 0
                || recalculatedAmount == null || recalculatedAmount.signum() < 0) {
            throw new IllegalArgumentException("期间重算金额不能为空或为负数");
        }
        BigDecimal signedDifference = recalculatedAmount.subtract(originalAmount);
        PremiumBalanceDirection direction = signedDifference.signum() > 0
                ? PremiumBalanceDirection.DEBIT
                : signedDifference.signum() < 0
                        ? PremiumBalanceDirection.CREDIT
                        : PremiumBalanceDirection.NONE;
        return new Difference(direction, signedDifference.abs());
    }

    public record Difference(PremiumBalanceDirection direction, BigDecimal amount) {
    }
}
