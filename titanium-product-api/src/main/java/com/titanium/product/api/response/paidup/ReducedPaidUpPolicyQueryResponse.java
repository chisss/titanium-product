package com.titanium.product.api.response.paidup;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product 减额缴清净费率策略只读查询结果（g04-05）。
 * <p>
 * 纯查表、零副作用：保全域减额缴清编排在业务时点取当前已发布策略，
 * 据「{@code 新保额 = 现金价值 ÷ 每元保额趸缴净费率}」核定降低后的保额。
 * </p>
 *
 * @param policyId 减额缴清策略ID
 * @param productId 产品ID
 * @param policyCode 减额缴清策略编码
 * @param policyVersion 减额缴清策略版本
 * @param netSinglePremiumRatePerUnit 每元保额趸缴净费率（必须为正；无上界——高龄保单可大于 1）
 * @param effectiveFrom 策略生效时间
 * @param effectiveTo 策略失效时间（空表示长期有效）
 * @param policyContentHash 策略内容哈希（取证用，可据其复核所用策略未被篡改）
 */
public record ReducedPaidUpPolicyQueryResponse(
        String policyId,
        String productId,
        String policyCode,
        String policyVersion,
        BigDecimal netSinglePremiumRatePerUnit,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        String policyContentHash) {

    public ReducedPaidUpPolicyQueryResponse {
        // 费率必须为正：反序列化即失败关闭，坏数据不出域、不被除零
        if (netSinglePremiumRatePerUnit == null || netSinglePremiumRatePerUnit.signum() <= 0) {
            throw new IllegalArgumentException("每元保额趸缴净费率必须大于零");
        }
    }
}
