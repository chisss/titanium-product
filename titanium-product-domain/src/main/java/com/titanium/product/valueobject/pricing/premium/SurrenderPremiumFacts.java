package com.titanium.product.valueobject.pricing.premium;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.exception.PricingDomainException;

/**
 * 未满期保费计算所需的保费事实（保单域下发，退费口径在本域）。
 * <p>
 * 未满期保费 = 年保费 × 剩余保障天数 / 365。三个分量**全部来自保单域**：年保费与「缴费是否
 * 期满」的解释权在保单域的缴费口径（{@code LinePaymentTerms} / {@code PremiumPlan}），
 * 保障止期是保单要素。本域只消费事实、不做口径推导——若在保全域或本域另立一套「期数 = 频次 ×
 * 年数」的算法，就是第二份保费口径（项目 CLAUDE.md §3.4.11 红线：同一判断两处各写一份必漂移）。
 * </p>
 * <p>
 * 🔴 <b>本值对象刻意不强制完整性</b>：这三个事实只有「退费基准 = 未满期保费」时才需要，
 * 现金价值基准的退费与只读现金价值查询都无需它们；若在构造期一律失败关闭，会让那些不需要
 * 这些事实的路径被牵连。完整性由 {@link #complete()} 显式暴露，由**使用点**
 * （{@code SurrenderValuePolicy} 的未满期保费分支）在真正需要时失败关闭。
 * </p>
 * <p>
 * 🔴 <b>允许年保费为 null</b>（保单未落缴费计划）：null 表示「不可得」，与 0 严格区分——
 * 0 表示「确定没有保费」，null 表示「取不到」。以 0 兜底会把取数失败伪装成「未满期保费为 0」。
 * </p>
 *
 * @param annualPremium   年保费（不可得时为 null；趸缴保单为整笔保费，其未满期部分由
 *                        {@code premiumPaidUp} 归零）
 * @param coverageEndDate 保障止期（终身型为 null；此为剩余保障天数的计算终点）
 * @param premiumPaidUp   缴费是否期满（含趸缴）：期满即无未满期保费可言，该项恒为 0
 */
public record SurrenderPremiumFacts(
        BigDecimal annualPremium,
        LocalDate coverageEndDate,
        boolean premiumPaidUp) {

    /** 只读现金价值查询等无保费语境的调用方使用：三项均不可得。 */
    public static final SurrenderPremiumFacts UNAVAILABLE = new SurrenderPremiumFacts(null, null, false);

    public SurrenderPremiumFacts {
        // 年保费若已取到必须非负：负的年保费会把未满期保费算成负额（AC-04 ③ 的直接防线）
        if (annualPremium != null && annualPremium.signum() < 0) {
            throw new PricingDomainException(ProductErrorCode.SURRENDER_FACTOR_INVALID,
                    "年保费不能为负数: " + annualPremium.toPlainString());
        }
    }

    /**
     * 未满期保费所需事实是否齐备。
     * <p>
     * 缴费期满时无需任何事实——该项由「期满」本身决定为 0。
     * </p>
     *
     * @return 齐备返回 {@code true}
     */
    public boolean complete() {
        return premiumPaidUp || (annualPremium != null && coverageEndDate != null);
    }
}
