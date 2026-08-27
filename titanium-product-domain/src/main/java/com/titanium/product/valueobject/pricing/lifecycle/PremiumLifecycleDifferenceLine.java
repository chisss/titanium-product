package com.titanium.product.valueobject.pricing.lifecycle;

import java.math.BigDecimal;
import java.util.Objects;

import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.enums.pricing.ChargePayerType;

/**
 * 生命周期变更前后的单项费用差额。
 */
public record PremiumLifecycleDifferenceLine(
        String lineId,
        String componentCode,
        String originalComponentVersion,
        String replacementComponentVersion,
        ChargeCategory category,
        AmountChannel amountChannel,
        ChargeDirection direction,
        ChargePayerType payerType,
        String accountingClass,
        String currency,
        ChargeDirection originalDirection,
        BigDecimal beforeAmount,
        ChargeDirection replacementDirection,
        BigDecimal afterAmount,
        BigDecimal differenceAmount,
        boolean customerVisible,
        boolean affectsCustomerPayable,
        String description) {

    public PremiumLifecycleDifferenceLine {
        requireText(lineId, "差额行ID");
        requireText(componentCode, "费用项编码");
        Objects.requireNonNull(category, "费用分类不能为空");
        Objects.requireNonNull(amountChannel, "金额通道不能为空");
        Objects.requireNonNull(direction, "差额方向不能为空");
        Objects.requireNonNull(payerType, "承担方不能为空");
        requireText(accountingClass, "账务分类");
        requireText(currency, "币种");
        requireNonNegative(beforeAmount, "变更前金额");
        requireNonNegative(afterAmount, "变更后金额");
        requirePositive(differenceAmount, "差额金额");
        requireDirection(originalDirection, beforeAmount, "变更前方向");
        requireDirection(replacementDirection, afterAmount, "变更后方向");
    }

    public BigDecimal signedDifference() {
        return direction == ChargeDirection.DEBIT ? differenceAmount : differenceAmount.negate();
    }

    /** 交换前后金额并反向，保证冲正行与原差额逐项守恒。 */
    public PremiumLifecycleDifferenceLine reverse() {
        return new PremiumLifecycleDifferenceLine(
                lineId, componentCode, replacementComponentVersion, originalComponentVersion,
                category, amountChannel, reverse(direction), payerType, accountingClass, currency,
                replacementDirection, afterAmount, originalDirection, beforeAmount, differenceAmount,
                customerVisible, affectsCustomerPayable, "冲正: " + description);
    }

    private ChargeDirection reverse(ChargeDirection value) {
        return value == ChargeDirection.DEBIT ? ChargeDirection.CREDIT : ChargeDirection.DEBIT;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "不能为空");
        }
    }

    private static void requireNonNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(field + "不能为负数");
        }
    }

    private static void requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(field + "必须大于0");
        }
    }

    private static void requireDirection(ChargeDirection direction, BigDecimal amount, String field) {
        if (amount.signum() > 0 && direction == null) {
            throw new IllegalArgumentException(field + "不能为空");
        }
    }
}
