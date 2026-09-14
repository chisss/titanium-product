package com.titanium.product.valueobject.pricing.pricing;

import java.math.BigDecimal;

/**
 * 精算基础参数值对象（精算公式模式专用）
 * <p>
 * 承载 {@code pricingMode=ACTUARIAL_FORMULA} 产品的精算三要素：预定利率、预定死亡率表引用、附加费用率。
 * </p>
 * <p>
 * 🔴 <b>未落地契约（m16-1904 实读定性，勿按「已建成的能力」施工）</b>：本对象<b>已贯通写入、
 * 持久化与回放</b>——命令入参（{@code CreateProductCommand} / {@code ReviseProductCommand}）→ 聚合字段装配
 * （{@code InsuranceProduct}）→ 领域事件（{@code ProductCreatedEvent} / {@code ProductRevisedEvent}）
 * → 读模型投影与后台上架回显（{@code ProductQueryResult} 等）；但<b>全域无任何计算消费点</b>：
 * 没有任何试算/定价链路读取这些参数，{@link #hasValidInterestRate()} 亦零调用。
 * {@code ACTUARIAL_FORMULA} 模式的保费实际由<b>外部规则工件</b>算出，与本对象无关
 * ⇒ 运营在后台配置的预定利率等参数<b>不会影响任何保费结果</b>，当前仅作配置留痕。
 * </p>
 * <p>
 * <b>接入前提</b>（三者齐备方可消解「未落地」定性）：① 精算计算领域服务落地（均衡净保费 APV / 年金因子）；
 * ② 计费域经端口取到本对象并作为计算入参（跨域取数契约，当前不存在）；③ 参数值域校验接入命令校验链
 * （如 {@link #hasValidInterestRate()}）。在此之前不得对外承诺精算口径。
 * </p>
 * <p>
 * ⚠️ <b>历史 javadoc 更正（m16-1904）</b>：原文称「提供 {@code LifePremiumCalculationDomainService}
 * 计算均衡净保费…billing 经 {@code ProductRatePort} 取到本对象作为入参传给精算领域服务」——
 * 这两个类型在<b>全仓均不存在</b>（除本注释自身外零命中，billing 侧 {@code actuarial} 亦零命中），
 * 属臆造的链路承诺，已删除。判据见规划文档「javadoc 承诺 ≠ 实际代码」。
 * </p>
 *
 * @param predefinedInterestRate 预定利率（如 0.03 表示 3%，精算贴现因子 v=1/(1+i) 基础）
 * @param mortalityTableRef      预定死亡率表引用编码（如 "CL2010-2013" 或 "CL_LIFE_2023"）
 * @param expenseLoadingRate     附加费用率（净保费×(1+expenseLoading)=毛保费，如 0.20 表示 20%）
 * @param reserveValuationRate   准备金评估利率（可选，评估精算准备金用；<b>null 表示与定价利率相同，
 *                               回落由消费方完成</b>——本值对象不做隐式取值）
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

    /**
     * 有效的预定利率（&gt;0 且 &lt;1）
     * <p>
     * ⚠️ 当前<b>零调用</b>（m16-1904 实读）——为接入精算计算的校验链预留的值域断言，
     * 尚未挂到任何命令校验上。接入计算链路时应在产品创建/修订的命令校验阶段调用。
     * </p>
     */
    public boolean hasValidInterestRate() {
        return predefinedInterestRate != null && predefinedInterestRate.compareTo(BigDecimal.ZERO) > 0
                && predefinedInterestRate.compareTo(BigDecimal.ONE) < 0;
    }
}
