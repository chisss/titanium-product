package com.titanium.product.application.model.pricing.loan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 保单贷款策略只读快照（应用层出参，g04-03）。
 * <p>
 * 由 {@code PolicyLoanPolicy} 纯查表得出，不落库、零副作用。承载策略取证信息
 * （编码/版本/内容哈希），使调用方可将「放款冻结的利率/比例」追溯到具体策略版本。
 * </p>
 *
 * @param policyId 策略ID
 * @param productId 产品ID
 * @param policyCode 策略编码
 * @param policyVersion 策略版本
 * @param loanableRatio 可贷比例
 * @param interestRate 贷款年利率
 * @param effectiveFrom 策略生效时间
 * @param effectiveTo 策略失效时间
 * @param policyContentHash 策略内容哈希
 */
public record LoanPolicySnapshot(
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
