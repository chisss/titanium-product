package com.titanium.product.valueobject;

import java.math.BigDecimal;

/**
 * 精算基础参数值对象（精算公式模式专用）
 * <p>
 * 提供 {@code LifePremiumCalculationDomainService} 计算均衡净保费（APV/年金因子）所需的精算参数。
 * billing 出单按产品 {@code pricingMode=ACTUARIAL_FORMULA} 时，经 {@code ProductRatePort}
 * 取到本对象作为入参传给精算领域服务。
 * </p>
 *
 * @param predefinedInterestRate 预定利率（如 0.03 表示 3%，精算贴现因子 v=1/(1+i) 基础）
 * @param mortalityTableRef      预定死亡率表引用编码（如 "CL2010-2013" 或 "CL_LIFE_2023"）
 * @param expenseLoadingRate     附加费用率（净保费×(1+expenseLoading)=毛保费，如 0.20 表示 20%）
 * @param reserveValuationRate   准备金评估利率（可选，评估精算准备金用，null 时取 predefinedInterestRate）
 */
public record ActuarialBasis(
        BigDecimal predefinedInterestRate,
        String     mortalityTableRef,
        BigDecimal expenseLoadingRate,
        BigDecimal reserveValuationRate
) {

    /**
     * 快速构造（评估利率与定价利率相同）
     *
     * @param predefinedInterestRate 预定利率
     * @param mortalityTableRef      死亡率表引用
     * @param expenseLoadingRate     附加费用率
     */
    public static ActuarialBasis of(BigDecimal predefinedInterestRate, String mortalityTableRef,
            BigDecimal expenseLoadingRate) {
        return new ActuarialBasis(predefinedInterestRate, mortalityTableRef, expenseLoadingRate, null);
    }

    /** 有效的预定利率（>0 且 <1） */
    public boolean hasValidInterestRate() {
        return predefinedInterestRate != null && predefinedInterestRate.compareTo(BigDecimal.ZERO) > 0
                && predefinedInterestRate.compareTo(BigDecimal.ONE) < 0;
    }
}
