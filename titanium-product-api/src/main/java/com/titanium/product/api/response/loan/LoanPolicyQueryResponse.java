package com.titanium.product.api.response.loan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product 保单贷款策略只读查询结果（g04-03）。
 * <p>
 * 纯查表、零副作用：保全域放款编排在业务时点取当前已发布策略，
 * 将可贷比例与年利率<b>冻结</b>进贷款账户后即可弃用本响应。
 * </p>
 *
 * @param policyId 贷款策略ID
 * @param productId 产品ID
 * @param policyCode 贷款策略编码
 * @param policyVersion 贷款策略版本
 * @param loanableRatio 可贷比例（0-1，如 0.8 表示最高可贷现金价值的 80%）
 * @param interestRate 贷款年利率（0-1，如 0.05 表示 5%）
 * @param effectiveFrom 策略生效时间
 * @param effectiveTo 策略失效时间（空表示长期有效）
 * @param policyContentHash 策略内容哈希（取证用，可据其复核所用策略未被篡改）
 */
public record LoanPolicyQueryResponse(
        String policyId,
        String productId,
        String policyCode,
        String policyVersion,
        BigDecimal loanableRatio,
        BigDecimal interestRate,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        String policyContentHash) {
}
