package com.titanium.product.valueobject.pricing.premium;

import java.math.BigDecimal;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.exception.PricingDomainException;

/**
 * 保单账户余额类事实（保单域下发，退费口径在本域）。
 * <p>
 * 退保时保单账上「尚未与客户结清的钱」共三项，分属两个退费因子：
 * <ul>
 *   <li>{@code overduePremium}——宽限期内欠缴保费，**客户欠公司**，退费时先行抵扣（{@code DEBIT}）；</li>
 *   <li>{@code dividendBalance} + {@code accountValue}——红利余额与投资账户价值，**公司欠客户**，
 *       退费时一并退还，合并为「红利及账户价值」一个因子（{@code CREDIT}）。</li>
 * </ul>
 * 三项全部来自保单域财务快照（g04-02 取数契约），本域只消费事实、不另立一套余额口径
 * （项目 CLAUDE.md §3.4.11 红线：同一判断两处各写一份必漂移）。
 * </p>
 * <p>
 * 🔴 <b>两个分量为何合并为一个因子</b>：{@code SurrenderFactorType.DIVIDEND_ACCOUNT_VALUE}
 * 是退费词表中的**唯一**客户权益余额因子；红利与账户价值同为「客户在保险公司账上的权益」，
 * 退保时一并退还，合并是**本域的退费口径决策**——故原始分量必须完整过桥到本域再由本域相加，
 * 保全域与适配器只搬运、不得先行求和（否则口径就落到了错误的一层）。
 * </p>
 * <p>
 * 🔴 <b>本值对象刻意不强制完整性、但强制同生同死</b>：账户余额事实只有「正式退费计算」才需要，
 * 只读现金价值查询（{@code cashValueAt}）无退费语境；若在构造期一律失败关闭，会把只读路径一并
 * 牵连。完整性由 {@link #complete()} 显式暴露，由**使用点**（{@code SurrenderValuePolicy} 的两个
 * 因子方法）在真正需要时失败关闭。但三项必须**同生同死**——只下发其中几项说明取数链路不完整，
 * 构造期即拒。
 * </p>
 * <p>
 * 🔴 <b>null 与 0 严格区分（AC-05 的承载点）</b>：null 表示「取不到」，0 表示「确定没有」。
 * 前者必须拒绝计算退费，后者是业务事实、正常按 0 呈现。以 0 兜底会把取数失败伪装成
 * 「无欠费、无余额」，让客户既拿不回红利、又逃掉欠缴抵扣。
 * </p>
 * <p>
 * 🔴 <b>「无该险种此概念」不是错误码</b>：非分红/非万能险的权益余额恒为 0（保单域已按业务事实
 * 下发 0），退费**正常计算**并按 0 因子呈现，**不得**与上一条的「取数失败」共用任何错误码。
 * </p>
 *
 * @param overduePremium  宽限期内欠缴保费（不可得时为 null；无欠费为 0）
 * @param dividendBalance 红利账户余额（不可得时为 null；未参与分红为 0）
 * @param accountValue    投资账户价值（不可得时为 null；未挂接投资账户为 0）
 */
public record SurrenderBalanceFacts(
        BigDecimal overduePremium,
        BigDecimal dividendBalance,
        BigDecimal accountValue) {

    /** 只读现金价值查询等无退费语境的调用方使用：三项均不可得。 */
    public static final SurrenderBalanceFacts UNAVAILABLE =
            new SurrenderBalanceFacts(null, null, null);

    public SurrenderBalanceFacts {
        // 同生同死约束：只给其中几项即取数不完整，拒绝在构造期而非静默把缺失项当 0
        boolean anyPresent = overduePremium != null || dividendBalance != null || accountValue != null;
        boolean allPresent = overduePremium != null && dividendBalance != null && accountValue != null;
        if (anyPresent && !allPresent) {
            throw new PricingDomainException(ProductErrorCode.SURRENDER_FACTOR_INVALID,
                    "欠缴保费、红利余额与投资账户价值必须同批下发：仅部分可得说明取数不完整");
        }
        requireNonNegative(overduePremium, "欠缴保费");
        requireNonNegative(dividendBalance, "红利账户余额");
        requireNonNegative(accountValue, "投资账户价值");
    }

    /**
     * 账户余额事实是否可得。
     *
     * @return 三项皆可得返回 {@code true}
     */
    public boolean complete() {
        return overduePremium != null;
    }

    /**
     * 红利及账户价值合计（即本次退费应退还客户的权益余额）。
     *
     * @return 红利余额 + 投资账户价值
     * @throws IllegalStateException 事实不可得时求和无意义
     */
    public BigDecimal totalDividendAccountValue() {
        if (!complete()) {
            throw new IllegalStateException("账户余额事实不可得，不得求和");
        }
        return dividendBalance.add(accountValue);
    }

    private static void requireNonNegative(BigDecimal amount, String field) {
        if (amount != null && amount.signum() < 0) {
            throw new PricingDomainException(ProductErrorCode.SURRENDER_FACTOR_INVALID,
                    field + "不能为负数: " + amount.toPlainString());
        }
    }
}
