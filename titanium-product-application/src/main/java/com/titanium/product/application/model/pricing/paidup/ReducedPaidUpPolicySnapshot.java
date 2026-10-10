package com.titanium.product.application.model.pricing.paidup;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 减额缴清净费率策略只读快照（应用层出参，g04-05）。
 * <p>
 * 由 {@code ReducedPaidUpPolicy} 纯查表得出，不落库、零副作用。承载策略取证信息
 * （编码/版本/内容哈希），使调用方可将「减额缴清核定所用的趸缴净费率」追溯到具体策略版本。
 * </p>
 *
 * @param policyId 策略ID
 * @param productId 产品ID
 * @param policyCode 策略编码
 * @param policyVersion 策略版本
 * @param netSinglePremiumRatePerUnit 每元保额趸缴净费率（必须为正，无上界）
 * @param effectiveFrom 策略生效时间
 * @param effectiveTo 策略失效时间
 * @param policyContentHash 策略内容哈希
 */
public record ReducedPaidUpPolicySnapshot(
        String policyId,
        String productId,
        String policyCode,
        String policyVersion,
        BigDecimal netSinglePremiumRatePerUnit,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        String policyContentHash) {
}
