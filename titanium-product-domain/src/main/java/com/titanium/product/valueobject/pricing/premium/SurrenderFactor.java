package com.titanium.product.valueobject.pricing.premium;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.SurrenderFactorType;
import com.titanium.product.exception.PricingDomainException;

/**
 * 退费因子：退保结算明细中的单项金额。
 * <p>
 * 🔴 金额恒为**非负绝对值**，增减由 {@link ChargeDirection} 表达——复用 metadata 既有的
 * {@code ChargeDirection}（DEBIT/CREDIT/NONE，语义与值域完全一致），**不新建同义枚举**，
 * 避免项目历史上「两套同义词汇长期漂移」的教训。
 * </p>
 * <p>
 * 🔴 {@code explainKey} 是**语言无关文案键**（形如 {@code surrender.factor.cashValue}），
 * 禁止中文字面量——文案由展示层按 locale 渲染（项目 CLAUDE.md §3.4.12 红线 20）。
 * 本值对象把该约束固化为构造期校验，使「把展示文案写进领域模型」在编译后的首次运行即失败。
 * </p>
 *
 * @param factorType 因子类型（现金价值/未满期保费/贷款本息/欠费/红利账户价值/手续费）
 * @param direction  金额方向：CREDIT 加项、DEBIT 扣项、NONE 该产品不适用
 * @param amount     金额绝对值（非负）
 * @param explainKey 文案键
 */
public record SurrenderFactor(
        SurrenderFactorType factorType,
        ChargeDirection direction,
        BigDecimal amount,
        String explainKey) {

    /** 中日韩表意文字：文案键里出现即说明把展示文案写进了领域模型 */
    private static final Pattern CJK = Pattern.compile("[\\u4e00-\\u9fff]");

    public SurrenderFactor {
        if (factorType == null || direction == null || amount == null) {
            throw invalid("退费因子的类型、方向、金额均不能为空");
        }
        if (amount.signum() < 0) {
            throw invalid("退费因子金额必须为非负绝对值，增减由 direction 表达");
        }
        if (explainKey == null || explainKey.isBlank()) {
            throw invalid("退费因子文案键不能为空");
        }
        if (CJK.matcher(explainKey).find()) {
            throw invalid("退费因子文案键禁止中文字面量: " + explainKey);
        }
        if (direction == ChargeDirection.NONE && amount.signum() != 0) {
            throw invalid("方向为 NONE 的退费因子表示「该产品不适用」，金额必须为 0");
        }
    }

    /**
     * 该因子对退费净额的带符号贡献：CREDIT 为正、DEBIT 为负、NONE 为零。
     *
     * @return 带符号金额
     */
    public BigDecimal signedAmount() {
        return switch (direction) {
            case CREDIT -> amount;
            case DEBIT -> amount.negate();
            case NONE -> BigDecimal.ZERO;
        };
    }

    private static PricingDomainException invalid(String detail) {
        return new PricingDomainException(ProductErrorCode.SURRENDER_FACTOR_INVALID, detail);
    }
}
