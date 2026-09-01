package com.titanium.product.api.response.calculation;

import java.math.BigDecimal;

/**
 * 可查询、可勾稽的费用计算明细。
 */
public record CalculationLineResponse(
        String lineId,
        String componentCode,
        String componentVersion,
        String category,
        String amountChannel,
        String direction,
        String payerType,
        String accountingClass,
        String currency,
        BigDecimal baseAmount,
        BigDecimal rate,
        BigDecimal calculatedAmount,
        String nodeCode,
        boolean customerVisible,
        String description,
        boolean affectsCustomerPayable,
        String jurisdictionCode,
        String regulatoryReferenceId,
        String taxPriceMode,
        String taxPolicyHash,
        Boolean taxExempt,
        String commissionChannelId,
        String commissionSchemeCode,
        String commissionSchemeVersion,
        String commissionSchemeHash,
        String commissionBeneficiaryType,
        String commissionBeneficiaryId,
        BigDecimal commissionSplitRate,
        BigDecimal commissionGrossAmount,
        Integer commissionInstallmentCount,
        Integer commissionClawbackMonths) {
}
