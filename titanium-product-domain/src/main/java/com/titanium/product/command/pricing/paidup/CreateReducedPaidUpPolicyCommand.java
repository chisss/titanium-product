package com.titanium.product.command.pricing.paidup;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 创建减额缴清净费率策略草稿命令（g04-05）。
 * <p>
 * 策略标识由应用层生成，命令只承载业务属性；创建后状态为 {@code DRAFT}，
 * 经审批（{@code approve}）→ 发布（{@code publish}）后方可供减额缴清核定消费。
 * </p>
 *
 * @param tenantId 租户ID
 * @param productId 产品ID
 * @param policyCode 策略编码
 * @param policyVersion 策略版本
 * @param netSinglePremiumRatePerUnit 每元保额趸缴净费率（必须为正，无上界）
 * @param effectiveFrom 生效时间
 * @param effectiveTo 失效时间（空表示长期有效）
 */
public record CreateReducedPaidUpPolicyCommand(
        String tenantId,
        String productId,
        String policyCode,
        String policyVersion,
        BigDecimal netSinglePremiumRatePerUnit,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo) {
}
