package com.titanium.product.command.pricing.surrender;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.titanium.product.common.enums.SurrenderRefundBasis;

/**
 * 创建退保价值策略草稿命令。
 * <p>
 * 策略标识由应用层生成，命令只承载业务属性；创建后状态为 {@code DRAFT}，
 * 经审批（{@code approve}）→ 发布（{@code publish}）后方可供退保价值计算消费。
 * </p>
 *
 * @param handlingFeeRate 退保手续费率；{@code null} 表示该产品**不收退保手续费**（不适用），
 *                        非 null 须落在 [0,1]。🔴 「不适用」与「配置为 0」是两种业务事实，
 *                        不得以下游默认 0 代替（g05-05 R-2 裁示）
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
        BigDecimal handlingFeeRate,
        SurrenderRefundBasis refundBasis,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo) {
}
