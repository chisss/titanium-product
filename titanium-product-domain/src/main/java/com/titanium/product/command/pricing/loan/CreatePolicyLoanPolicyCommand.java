package com.titanium.product.command.pricing.loan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建保单贷款策略草稿命令（g04-03）。
 * <p>
 * 策略标识由应用层生成，命令只承载业务属性；创建后状态为 {@code DRAFT}，
 * 经审批（{@code approve}）→ 发布（{@code publish}）后方可供放款定价消费。
 * </p>
 */
public record CreatePolicyLoanPolicyCommand(
        String tenantId,
        String productId,
        String policyCode,
        String policyVersion,
        BigDecimal loanableRatio,
        BigDecimal interestRate,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo) {
}
