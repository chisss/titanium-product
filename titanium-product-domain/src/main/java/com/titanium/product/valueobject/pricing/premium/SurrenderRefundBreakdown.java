package com.titanium.product.valueobject.pricing.premium;

import java.math.BigDecimal;
import java.util.List;

import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.SurrenderFactorType;
import com.titanium.product.exception.PricingDomainException;

/**
 * 退费明细：退费因子列表与其加总结果。
 * <p>
 * 🔴 **加总守卫内聚在本值对象**（单一职责）：净额恒等于 {@code ΣCREDIT − ΣDEBIT}，
 * 毛额/扣项/手续费/净额**一律由因子列表重算**并在构造期与传入值比对——任何
 * 「调用方自报金额」或「明细与净额漂移」在构造期即失败，不得把该校验散落在各 service。
 * </p>
 * <p>
 * 🔴 净额为负**一律拒绝**（错误码 {@code SURRENDER_NET_AMOUNT_NEGATIVE}）：扣项合计超过
 * 应退金额时不得生成负额退款指令，须转人工核价（需求文档 R-10）。
 * </p>
 * <p>
 * 金额比较一律用 {@code compareTo}（数值相等）而非 {@code equals}——{@code BigDecimal}
 * 的 {@code equals} 对 scale 敏感，{@code 100} 与 {@code 100.00} 判为不等会产出假红。
 * </p>
 *
 * @param factors                退费因子列表（不可为空、至少一项）
 * @param grossAmount            毛额：ΣCREDIT
 * @param deductions             扣项合计：ΣDEBIT
 * @param feeAmount              手续费合计：HANDLING_FEE 因子的金额绝对值之和
 * @param netAmount              净额：grossAmount − deductions，即实际应退客户金额
 * @param retainedCustomerAmount 客户保留金额（已缴保费中未退还部分）
 */
public record SurrenderRefundBreakdown(
        List<SurrenderFactor> factors,
        BigDecimal grossAmount,
        BigDecimal deductions,
        BigDecimal feeAmount,
        BigDecimal netAmount,
        BigDecimal retainedCustomerAmount) {

    public SurrenderRefundBreakdown {
        if (factors == null || factors.isEmpty()) {
            throw invalid("退费明细的因子列表不能为空（至少一项，金额可为 0）");
        }
        factors = List.copyOf(factors);
        BigDecimal gross = sumOf(factors, ChargeDirection.CREDIT);
        BigDecimal deduction = sumOf(factors, ChargeDirection.DEBIT);
        BigDecimal fee = feeOf(factors);
        BigDecimal net = gross.subtract(deduction);
        requireSame(grossAmount, gross, "退费毛额");
        requireSame(deductions, deduction, "退费扣项合计");
        requireSame(feeAmount, fee, "退费手续费合计");
        requireSame(netAmount, net, "退费净额");
        if (retainedCustomerAmount == null || retainedCustomerAmount.signum() < 0) {
            throw invalid("客户保留金额不能为空或负数");
        }
        if (net.signum() < 0) {
            throw new PricingDomainException(
                    ProductErrorCode.SURRENDER_NET_AMOUNT_NEGATIVE,
                    "退费扣项合计 " + deduction.toPlainString() + " 超过应退合计 " + gross.toPlainString()
                            + "，净额为 " + net.toPlainString());
        }
    }

    /**
     * 由因子列表与客户保留额构造：毛额、扣项、手续费、净额一律由因子加总产生。
     *
     * @param factors                退费因子列表
     * @param retainedCustomerAmount 客户保留金额
     * @return 退费明细
     */
    public static SurrenderRefundBreakdown of(
            List<SurrenderFactor> factors, BigDecimal retainedCustomerAmount) {
        List<SurrenderFactor> copy = factors == null ? List.of() : List.copyOf(factors);
        BigDecimal gross = sumOf(copy, ChargeDirection.CREDIT);
        BigDecimal deduction = sumOf(copy, ChargeDirection.DEBIT);
        return new SurrenderRefundBreakdown(copy, gross, deduction, feeOf(copy),
                gross.subtract(deduction), retainedCustomerAmount);
    }

    /** 指定方向的因子金额绝对值合计 */
    private static BigDecimal sumOf(List<SurrenderFactor> factors, ChargeDirection direction) {
        return factors.stream()
                .filter(factor -> factor.direction() == direction)
                .map(SurrenderFactor::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 手续费因子金额绝对值合计（方向由因子自身表达，此处只取金额规模） */
    private static BigDecimal feeOf(List<SurrenderFactor> factors) {
        return factors.stream()
                .filter(factor -> factor.factorType() == SurrenderFactorType.HANDLING_FEE)
                .map(SurrenderFactor::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void requireSame(BigDecimal actual, BigDecimal expected, String field) {
        if (actual == null || actual.compareTo(expected) != 0) {
            throw invalid(field + "与因子加总不一致：传入 "
                    + (actual == null ? "null" : actual.toPlainString())
                    + "，加总为 " + expected.toPlainString());
        }
    }

    private static PricingDomainException invalid(String detail) {
        return new PricingDomainException(ProductErrorCode.SURRENDER_FACTOR_INVALID, detail);
    }
}
