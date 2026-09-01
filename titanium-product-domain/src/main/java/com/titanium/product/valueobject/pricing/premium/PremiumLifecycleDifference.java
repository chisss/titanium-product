package com.titanium.product.valueobject.pricing.premium;

import java.math.BigDecimal;
import java.util.List;

import com.titanium.product.common.enums.PremiumBalanceDirection;

/**
 * 生命周期变更的客户价格、税费和内部成本差额。
 */
public record PremiumLifecycleDifference(
        PremiumBalanceDirection direction,
        BigDecimal customerAmount,
        PremiumBalanceDirection taxDirection,
        BigDecimal taxAmount,
        PremiumBalanceDirection internalCostDirection,
        BigDecimal internalCostAmount,
        List<PremiumLifecycleDifferenceLine> lines) {

    public PremiumLifecycleDifference {
        if (direction == null || taxDirection == null || internalCostDirection == null
                || customerAmount == null || taxAmount == null || internalCostAmount == null
                || customerAmount.signum() < 0 || taxAmount.signum() < 0 || internalCostAmount.signum() < 0) {
            throw new IllegalArgumentException("生命周期差额汇总不合法");
        }
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    /** 将客户、税费、内部成本和结构化费用行全部反向，生成冲正差额。 */
    public PremiumLifecycleDifference reverse() {
        return new PremiumLifecycleDifference(
                reverse(direction), customerAmount, reverse(taxDirection), taxAmount,
                reverse(internalCostDirection), internalCostAmount,
                lines.stream().map(PremiumLifecycleDifferenceLine::reverse).toList());
    }

    private PremiumBalanceDirection reverse(PremiumBalanceDirection value) {
        return switch (value) {
            case DEBIT -> PremiumBalanceDirection.CREDIT;
            case CREDIT -> PremiumBalanceDirection.DEBIT;
            case NONE -> PremiumBalanceDirection.NONE;
        };
    }
}
