package com.titanium.product.valueobject.pricing.calculation;

import java.math.BigDecimal;

import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.product.valueobject.pricing.commission.CommissionLineEvidence;
import com.titanium.product.valueobject.pricing.premium.TaxLineEvidence;

/**
 * 可查询、可勾稽的单项费用计算事实。
 */
public record CalculationLine(
        String lineId,
        String componentCode,
        String componentVersion,
        ChargeCategory category,
        AmountChannel amountChannel,
        ChargeDirection direction,
        ChargePayerType payerType,
        String accountingClass,
        String currency,
        BigDecimal baseAmount,
        BigDecimal rate,
        BigDecimal calculatedAmount,
        String nodeCode,
        boolean customerVisible,
        String description,
        boolean affectsCustomerPayable,
        TaxLineEvidence taxEvidence,
        CommissionLineEvidence commissionEvidence) {

    public CalculationLine {
        if (lineId == null || lineId.isBlank() || componentCode == null || componentCode.isBlank()
                || componentVersion == null || componentVersion.isBlank() || category == null
                || amountChannel == null || direction == null || payerType == null
                || accountingClass == null || accountingClass.isBlank() || currency == null
                || currency.length() != 3 || calculatedAmount == null || calculatedAmount.signum() < 0
                || nodeCode == null || nodeCode.isBlank()) {
            throw new IllegalArgumentException("结构化费用明细字段不完整或金额不合法");
        }
    }

    /**
     * 兼容 V2-B 调用方；既有完整费用行默认不携带佣金证据。
     */
    public CalculationLine(
            String lineId,
            String componentCode,
            String componentVersion,
            ChargeCategory category,
            AmountChannel amountChannel,
            ChargeDirection direction,
            ChargePayerType payerType,
            String accountingClass,
            String currency,
            BigDecimal baseAmount,
            BigDecimal rate,
            BigDecimal calculatedAmount,
            String nodeCode,
            boolean customerVisible,
            String description,
            boolean affectsCustomerPayable,
            TaxLineEvidence taxEvidence) {
        this(lineId, componentCode, componentVersion, category, amountChannel, direction, payerType,
                accountingClass, currency, baseAmount, rate, calculatedAmount, nodeCode, customerVisible,
                description, affectsCustomerPayable, taxEvidence, null);
    }

    /**
     * 兼容 V2-A 调用方；既有费用行默认影响客户应付且不携带税务证据。
     */
    public CalculationLine(
            String lineId,
            String componentCode,
            String componentVersion,
            ChargeCategory category,
            AmountChannel amountChannel,
            ChargeDirection direction,
            ChargePayerType payerType,
            String accountingClass,
            String currency,
            BigDecimal baseAmount,
            BigDecimal rate,
            BigDecimal calculatedAmount,
            String nodeCode,
            boolean customerVisible,
            String description) {
        this(lineId, componentCode, componentVersion, category, amountChannel, direction, payerType,
                accountingClass, currency, baseAmount, rate, calculatedAmount, nodeCode, customerVisible,
                description, true, null, null);
    }

    public BigDecimal signedAmount() {
        return direction == ChargeDirection.DEBIT ? calculatedAmount : calculatedAmount.negate();
    }
}
