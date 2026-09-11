package com.titanium.product.application.model.pricing.surrender;

import java.math.BigDecimal;

/**
 * 保单现金价值只读快照（应用层出参）。
 * <p>
 * 由 {@code SurrenderValuePolicy} 纯查表计算得出，不落库、不产生生命周期贷项。承载策略取证信息
 * （编码/版本/内容哈希），使调用方可将「算出来的现金价值」追溯到具体策略版本。
 * </p>
 *
 * @param productId 产品ID
 * @param policyYear 保单年度
 * @param coolingOffDays 犹豫期天数
 * @param withinCoolingOff 估值日是否落在犹豫期内
 * @param refundType 退保类型
 * @param cashValueRate 适用现金价值率
 * @param cashValue 现金价值金额
 * @param policyCode 策略编码
 * @param policyVersion 策略版本
 * @param policyContentHash 策略内容哈希
 */
public record CashValueSnapshot(
        String productId,
        int policyYear,
        int coolingOffDays,
        boolean withinCoolingOff,
        String refundType,
        BigDecimal cashValueRate,
        BigDecimal cashValue,
        String policyCode,
        String policyVersion,
        String policyContentHash) {
}
