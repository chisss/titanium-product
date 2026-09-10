package com.titanium.product.command.pricing.surrender;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建退保价值策略草稿命令。
 * <p>
 * 策略标识由应用层生成，命令只承载业务属性；创建后状态为 {@code DRAFT}，
 * 经审批（{@code approve}）→ 发布（{@code publish}）后方可供退保价值计算消费。
 * </p>
 */
public record CreateSurrenderValuePolicyCommand(
        String tenantId,
        String productId,
        String policyCode,
        String policyVersion,
        int policyYear,
        int coolingOffDays,
        BigDecimal cashValueRate,
        BigDecimal internalCostRetentionRate,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo) {
}
