package com.titanium.product.valueobject.pricing.premium;

import java.math.BigDecimal;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.exception.PricingDomainException;

/**
 * 保单贷款未偿本息事实（保单域下发，抵扣口径在本域）。
 * <p>
 * 退保时客户对保险公司的未偿贷款**先行抵扣**应退金额：退费明细中的抵扣额 = 未偿本金 + 未偿利息。
 * 两个分量**全部来自保单域的贷款账户**（按 g04-03 同一 {@code PolicyLoanDetail} 口径聚合，
 * 「已过账余额」），本域只消费事实、不另立一套计息/余额算法（项目 CLAUDE.md §3.4.11 红线：
 * 同一判断两处各写一份必漂移）。
 * </p>
 * <p>
 * 🔴 <b>本值对象刻意不强制完整性、但强制成对</b>：贷款事实只有「正式退费计算」才需要，
 * 只读现金价值查询（{@code cashValueAt}）无退费语境、无贷款可言；若在构造期一律失败关闭，
 * 会把只读路径一并牵连。完整性由 {@link #complete()} 显式暴露，由**使用点**
 * （{@code SurrenderValuePolicy} 的贷款因子）在真正需要时失败关闭。
 * 但**两项必须同生同死**——只下发一项说明取数链路不完整，构造期即拒。
 * </p>
 * <p>
 * 🔴 <b>null 与 0 严格区分（AC-05 的承载点）</b>：null 表示「取不到」，0 表示「确定无未结贷款」。
 * 前者必须拒绝计算退费，后者是业务事实、正常抵扣 0。以 0 兜底会把取数失败伪装成「无贷款」，
 * 让客户在贷款未偿时拿到全额退费。
 * </p>
 *
 * @param outstandingPrincipal 未偿本金（不可得时为 null；无未结贷款为 0）
 * @param outstandingInterest  未偿利息（不可得时为 null；无未结贷款为 0）
 */
public record SurrenderLoanFacts(
        BigDecimal outstandingPrincipal,
        BigDecimal outstandingInterest) {

    /** 只读现金价值查询等无退费语境的调用方使用：两项均不可得。 */
    public static final SurrenderLoanFacts UNAVAILABLE = new SurrenderLoanFacts(null, null);

    public SurrenderLoanFacts {
        // 成对约束：只给一项即取数不完整，拒绝在构造期而非静默把缺失项当 0
        if ((outstandingPrincipal == null) != (outstandingInterest == null)) {
            throw new PricingDomainException(ProductErrorCode.SURRENDER_FACTOR_INVALID,
                    "贷款未偿本金与未偿利息必须成对下发：仅一项可得说明取数不完整");
        }
        requireNonNegative(outstandingPrincipal, "贷款未偿本金");
        requireNonNegative(outstandingInterest, "贷款未偿利息");
    }

    /**
     * 贷款事实是否可得。
     *
     * @return 两项皆可得返回 {@code true}
     */
    public boolean complete() {
        return outstandingPrincipal != null;
    }

    /**
     * 未偿本息合计（即本次退费应先行抵扣的金额）。
     *
     * @return 本金 + 利息
     * @throws IllegalStateException 事实不可得时求和无意义
     */
    public BigDecimal totalOutstanding() {
        if (!complete()) {
            throw new IllegalStateException("贷款事实不可得，不得求和");
        }
        return outstandingPrincipal.add(outstandingInterest);
    }

    private static void requireNonNegative(BigDecimal amount, String field) {
        if (amount != null && amount.signum() < 0) {
            throw new PricingDomainException(ProductErrorCode.SURRENDER_FACTOR_INVALID,
                    field + "不能为负数: " + amount.toPlainString());
        }
    }
}
