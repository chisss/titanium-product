package com.titanium.product.valueobject.pricing.premium;

import java.math.BigDecimal;
import java.util.List;

import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.SurrenderFactorType;
import com.titanium.product.common.enums.SurrenderRefundType;
import com.titanium.product.exception.PricingDomainException;

/**
 * 退保价值策略对一张保单产生的确定性结果。
 * <p>
 * 🔴 结果**必须携带退费明细**（{@link SurrenderRefundBreakdown}）：应退金额不再是一次乘法的
 * 直接产物，而是因子加总的结果（{@code refundAmount == breakdown.netAmount()}，构造期校验）。
 * 这样「实付为何少于现金价值」可由明细逐项解释（扣贷款本息、欠费、手续费，加未满期保费、
 * 红利账户价值），运营与对账方读得到口径。
 * </p>
 * <p>
 * 🔴 **保留 6 参兼容构造器**：旧调用方（只关心现金价值率与应退金额）仍可用，明细由其按
 * 「现金价值」单因子补齐——避免跨域契约与既有用例大面积改名（兼容先例见
 * {@code ProductSurrenderValuePort.SurrenderFact}）。
 * </p>
 *
 * @param refundType                退费类型（犹豫期全额退 / 现金价值退 / 未满期保费退）
 * @param withinCoolingOff          是否犹豫期内
 * @param cashValueRate             实际适用的退费率（犹豫期内为 1）
 * @param refundAmount              应退金额（= 明细净额）
 * @param retainedCustomerAmount    客户保留金额
 * @param internalCostRetentionRate 内部成本留存率
 * @param breakdown                 退费明细（因子列表 + 加总）
 */
public record SurrenderValueOutcome(
        SurrenderRefundType refundType,
        boolean withinCoolingOff,
        BigDecimal cashValueRate,
        BigDecimal refundAmount,
        BigDecimal retainedCustomerAmount,
        BigDecimal internalCostRetentionRate,
        SurrenderRefundBreakdown breakdown) {

    /** 犹豫期全额退费因子的文案键 */
    public static final String EXPLAIN_COOLING_OFF_FULL_REFUND = "surrender.factor.coolingOffFullRefund";
    /** 现金价值因子的文案键 */
    public static final String EXPLAIN_CASH_VALUE = "surrender.factor.cashValue";
    /** 未满期保费因子的文案键 */
    public static final String EXPLAIN_UNEARNED_PREMIUM = "surrender.factor.unearnedPremium";
    /** 保单贷款未偿本息因子的文案键 */
    public static final String EXPLAIN_LOAN_PRINCIPAL_INTEREST = "surrender.factor.loanPrincipalInterest";
    /** 欠缴保费因子的文案键 */
    public static final String EXPLAIN_OVERDUE_PREMIUM = "surrender.factor.overduePremium";
    /** 红利及账户价值因子的文案键 */
    public static final String EXPLAIN_DIVIDEND_ACCOUNT_VALUE = "surrender.factor.dividendAccountValue";
    /** 退保手续费因子的文案键 */
    public static final String EXPLAIN_HANDLING_FEE = "surrender.factor.handlingFee";

    public SurrenderValueOutcome {
        if (refundType == null || cashValueRate == null || internalCostRetentionRate == null) {
            throw invalid("退费类型、适用退费率与内部成本留存率均不能为空");
        }
        if (breakdown == null) {
            throw invalid("退费明细不能为空：应退金额必须由因子加总得出");
        }
        requireSame(refundAmount, breakdown.netAmount(), "应退金额");
        requireSame(retainedCustomerAmount, breakdown.retainedCustomerAmount(), "客户保留金额");
    }

    /**
     * 兼容构造器：明细按「现金价值」单因子组装（旧调用方无因子语境）。
     *
     * @param refundType                退费类型
     * @param withinCoolingOff          是否犹豫期内
     * @param cashValueRate             实际适用的退费率
     * @param refundAmount              应退金额
     * @param retainedCustomerAmount    客户保留金额
     * @param internalCostRetentionRate 内部成本留存率
     */
    public SurrenderValueOutcome(
            SurrenderRefundType refundType,
            boolean withinCoolingOff,
            BigDecimal cashValueRate,
            BigDecimal refundAmount,
            BigDecimal retainedCustomerAmount,
            BigDecimal internalCostRetentionRate) {
        this(refundType, withinCoolingOff, cashValueRate, refundAmount, retainedCustomerAmount,
                internalCostRetentionRate,
                singleCashValueBreakdown(refundType, refundAmount, retainedCustomerAmount));
    }

    /**
     * 退费类型对应的因子文案键（展示层按 locale 渲染，领域模型只存键）。
     *
     * @param refundType 退费类型
     * @return 文案键
     */
    public static String explainKeyOf(SurrenderRefundType refundType) {
        return switch (refundType) {
            case COOLING_OFF_FULL_REFUND -> EXPLAIN_COOLING_OFF_FULL_REFUND;
            case UNEARNED_PREMIUM -> EXPLAIN_UNEARNED_PREMIUM;
            case CASH_VALUE -> EXPLAIN_CASH_VALUE;
        };
    }

    private static SurrenderRefundBreakdown singleCashValueBreakdown(
            SurrenderRefundType refundType, BigDecimal refundAmount, BigDecimal retainedCustomerAmount) {
        if (refundType == null || refundAmount == null) {
            throw invalid("退费类型与应退金额不能为空");
        }
        return SurrenderRefundBreakdown.of(
                List.of(new SurrenderFactor(SurrenderFactorType.CASH_VALUE, ChargeDirection.CREDIT,
                        refundAmount, explainKeyOf(refundType))),
                retainedCustomerAmount);
    }

    private static void requireSame(BigDecimal actual, BigDecimal expected, String field) {
        if (actual == null || actual.compareTo(expected) != 0) {
            throw invalid(field + "与退费明细不一致：结果 " + (actual == null ? "null" : actual.toPlainString())
                    + "，明细 " + expected.toPlainString());
        }
    }

    private static PricingDomainException invalid(String detail) {
        return new PricingDomainException(ProductErrorCode.SURRENDER_FACTOR_INVALID, detail);
    }
}
