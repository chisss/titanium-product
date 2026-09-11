package com.titanium.product.api.response.premium;

import java.math.BigDecimal;

/**
 * Product 保单现金价值只读查询结果。
 * <p>
 * 与 {@link SurrenderValueCalculationResponse} 的区别：本响应**纯查表计算、零副作用**——不基于出单确认
 * 计算记录、不创建 {@code PremiumCalculation} 与生命周期贷项，供上游（保单域）按日扫描等高频只读场景取值。
 * 退保价值确认（有副作用、需落库）仍走 {@code /premium-calculations/surrender-values}。
 * </p>
 *
 * @param productId 产品ID
 * @param policyYear 保单年度（自 1 起）
 * @param coolingOffDays 犹豫期天数（命中则全额退还，现金价值率视为 1）
 * @param withinCoolingOff 估值日是否落在犹豫期内
 * @param refundType 退保类型（{@code COOLING_OFF_FULL_REFUND} 犹豫期全额退还 / {@code CASH_VALUE} 按现金价值）
 * @param cashValueRate 适用的现金价值率
 * @param cashValue 现金价值金额（= 传入的客户应付保费基数 × 现金价值率，犹豫期内为全额）
 * @param policyCode 退保价值策略编码（取证用）
 * @param policyVersion 退保价值策略版本（取证用）
 * @param policyContentHash 退保价值策略内容哈希（取证用，可据其复核所用策略未被篡改）
 */
public record CashValueQueryResponse(
        String productId,
        Integer policyYear,
        Integer coolingOffDays,
        boolean withinCoolingOff,
        String refundType,
        BigDecimal cashValueRate,
        BigDecimal cashValue,
        String policyCode,
        String policyVersion,
        String policyContentHash) {
}
