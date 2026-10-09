package com.titanium.product.api.response.premium;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product 不可变退保价值和生命周期贷项结果。
 *
 * @param surrenderRequestId 退保请求ID（幂等键）
 * @param policyCode 退保价值策略编码
 * @param policyVersion 退保价值策略版本
 * @param policyContentHash 退保价值策略内容哈希（取证用）
 * @param policyYear 保单年度（自 1 起）
 * @param coolingOffDays 犹豫期天数
 * @param refundBasis 退费基准（CASH_VALUE / UNEARNED_PREMIUM）：两个因子恒在明细中，未生效者为 0
 * @param refundType 退费类型（COOLING_OFF_FULL_REFUND / CASH_VALUE / UNEARNED_PREMIUM）
 * @param withinCoolingOff 是否犹豫期内
 * @param cashValueRate 实际适用的退费率（犹豫期内为 1）
 * @param refundAmount 应退金额（= 退费明细净额）
 * @param retainedCustomerAmount 客户保留金额
 * @param internalCostRetentionRate 内部成本留存率
 * @param factors 退费明细因子列表（毛额/扣项/手续费/净额由因子加总产生，供对客解释与对账）
 * @param originalCalculationId 出单确认计算ID
 * @param originalResultHash 出单确认计算结果哈希
 * @param replacementCalculationId 替代计算ID
 * @param replacementResultHash 替代计算结果哈希
 * @param adjustmentId 生命周期调整ID
 * @param requestHash 请求哈希（幂等冲突判定）
 * @param adjustmentResultHash 生命周期调整结果哈希
 * @param pricingPlanVersion 定价方案版本
 * @param pricingPlanContentHash 定价方案内容哈希
 * @param direction 资金方向（DEBIT / CREDIT / NONE）
 * @param amount 资金金额
 * @param currency 币种
 */
public record SurrenderValueCalculationResponse(
        String surrenderRequestId,
        String policyCode,
        String policyVersion,
        String policyContentHash,
        Integer policyYear,
        Integer coolingOffDays,
        String refundBasis,
        String refundType,
        boolean withinCoolingOff,
        BigDecimal cashValueRate,
        BigDecimal refundAmount,
        BigDecimal retainedCustomerAmount,
        BigDecimal internalCostRetentionRate,
        List<SurrenderFactorResponse> factors,
        String originalCalculationId,
        String originalResultHash,
        String replacementCalculationId,
        String replacementResultHash,
        String adjustmentId,
        String requestHash,
        String adjustmentResultHash,
        String pricingPlanVersion,
        String pricingPlanContentHash,
        String direction,
        BigDecimal amount,
        String currency) {
}
