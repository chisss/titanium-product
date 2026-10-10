package com.titanium.product.valueobject.pricing.premium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.SurrenderFactorType;
import com.titanium.product.exception.PricingDomainException;

/**
 * 退费明细值对象契约测试（g05-01）
 * <p>
 * 本类固化「加总守卫内聚于值对象」这一设计：净额恒等于 {@code ΣCREDIT − ΣDEBIT}，
 * 毛额/扣项/手续费/净额四个汇总值一律由因子列表<b>重算并比对</b>，任何自报金额或明细漂移
 * 在构造期即失败——故断言的重点是<b>守卫真的拦得住</b>（漂移即抛），而非仅算得对。
 * </p>
 */
class SurrenderRefundBreakdownTest {

    @Test
    @DisplayName("五因子加总：净额 = ΣCREDIT − ΣDEBIT，手续费单列")
    void shouldSumFactorsIntoNetAmount() {
        SurrenderRefundBreakdown breakdown = SurrenderRefundBreakdown.of(List.of(
                credit(SurrenderFactorType.CASH_VALUE, "1000.00"),
                credit(SurrenderFactorType.UNEARNED_PREMIUM, "200.00"),
                debit(SurrenderFactorType.HANDLING_FEE, "50.00"),
                debit(SurrenderFactorType.LOAN_PRINCIPAL_INTEREST, "80.00"),
                none(SurrenderFactorType.DIVIDEND_ACCOUNT_VALUE)), new BigDecimal("170.00"));

        assertEquals(0, breakdown.grossAmount().compareTo(new BigDecimal("1200.00")));
        assertEquals(0, breakdown.deductions().compareTo(new BigDecimal("130.00")));
        assertEquals(0, breakdown.feeAmount().compareTo(new BigDecimal("50.00")),
                "手续费合计只取 HANDLING_FEE 因子，不并入其它扣项");
        assertEquals(0, breakdown.netAmount().compareTo(new BigDecimal("1070.00")));
        assertEquals(0, breakdown.retainedCustomerAmount().compareTo(new BigDecimal("170.00")));
        assertEquals(5, breakdown.factors().size(), "因子明细须逐项保留，供对客解释与对账");
    }

    @Test
    @DisplayName("毛额与因子加总不符即拒绝：禁止调用方自报金额")
    void shouldRejectGrossAmountDriftedFromFactors() {
        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> new SurrenderRefundBreakdown(List.of(credit(SurrenderFactorType.CASH_VALUE, "1000.00")),
                        new BigDecimal("1000.01"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1000.00"),
                        BigDecimal.ZERO));

        assertEquals(ProductErrorCode.SURRENDER_FACTOR_INVALID, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("退费毛额"), "报错须点名漂移的字段，否则无从定位");
    }

    @Test
    @DisplayName("扣项合计与因子加总不符即拒绝")
    void shouldRejectDeductionsDriftedFromFactors() {
        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> new SurrenderRefundBreakdown(
                        List.of(credit(SurrenderFactorType.CASH_VALUE, "1000.00"),
                                debit(SurrenderFactorType.HANDLING_FEE, "50.00")),
                        new BigDecimal("1000.00"), new BigDecimal("49.99"), new BigDecimal("50.00"),
                        new BigDecimal("950.00"), BigDecimal.ZERO));

        assertEquals(ProductErrorCode.SURRENDER_FACTOR_INVALID, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("退费扣项合计"));
    }

    @Test
    @DisplayName("净额与因子加总不符即拒绝：明细与净额不得漂移")
    void shouldRejectNetAmountDriftedFromFactors() {
        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> new SurrenderRefundBreakdown(
                        List.of(credit(SurrenderFactorType.CASH_VALUE, "1000.00"),
                                debit(SurrenderFactorType.HANDLING_FEE, "50.00")),
                        new BigDecimal("1000.00"), new BigDecimal("50.00"), new BigDecimal("50.00"),
                        new BigDecimal("949.99"), BigDecimal.ZERO));

        assertEquals(ProductErrorCode.SURRENDER_FACTOR_INVALID, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("退费净额"));
    }

    @Test
    @DisplayName("手续费合计与 HANDLING_FEE 因子不符即拒绝")
    void shouldRejectFeeAmountDriftedFromFactors() {
        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> new SurrenderRefundBreakdown(
                        List.of(credit(SurrenderFactorType.CASH_VALUE, "1000.00"),
                                debit(SurrenderFactorType.HANDLING_FEE, "50.00")),
                        new BigDecimal("1000.00"), new BigDecimal("50.00"), new BigDecimal("50.01"),
                        new BigDecimal("950.00"), BigDecimal.ZERO));

        assertEquals(ProductErrorCode.SURRENDER_FACTOR_INVALID, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("退费手续费合计"));
    }

    @Test
    @DisplayName("扣项超过应退合计即拒绝，不得生成负额退款指令（需求 R-10）")
    void shouldRejectNegativeNetAmount() {
        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> SurrenderRefundBreakdown.of(
                        List.of(credit(SurrenderFactorType.CASH_VALUE, "100.00"),
                                debit(SurrenderFactorType.LOAN_PRINCIPAL_INTEREST, "150.00")),
                        BigDecimal.ZERO));

        assertEquals(ProductErrorCode.SURRENDER_NET_AMOUNT_NEGATIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("净额恰为 0 合法：全额抵扣场景不应被负额守卫误伤")
    void shouldAcceptZeroNetAmount() {
        SurrenderRefundBreakdown breakdown = SurrenderRefundBreakdown.of(
                List.of(credit(SurrenderFactorType.CASH_VALUE, "100.00"),
                        debit(SurrenderFactorType.OVERDUE_PREMIUM, "100.00")),
                BigDecimal.ZERO);

        assertEquals(0, breakdown.netAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("金额比较按数值而非 scale：100 与 100.00 视为一致，不产出假红")
    void shouldCompareAmountsIgnoringScale() {
        SurrenderRefundBreakdown breakdown = new SurrenderRefundBreakdown(
                List.of(credit(SurrenderFactorType.CASH_VALUE, "100")),
                new BigDecimal("100.00"), new BigDecimal("0.000"), new BigDecimal("0.00"),
                new BigDecimal("100.0000"), new BigDecimal("0"));

        assertEquals(0, breakdown.netAmount().compareTo(new BigDecimal("100")));
    }

    @Test
    @DisplayName("因子列表为空即拒绝：明细至少一项（金额可为 0）")
    void shouldRejectEmptyFactorList() {
        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> SurrenderRefundBreakdown.of(List.of(), BigDecimal.ZERO));

        assertEquals(ProductErrorCode.SURRENDER_FACTOR_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("因子列表不可变：外部改动不得渗透进已构造的明细")
    void shouldDefensivelyCopyFactorList() {
        List<SurrenderFactor> mutable = new ArrayList<>();
        mutable.add(credit(SurrenderFactorType.CASH_VALUE, "1000.00"));
        SurrenderRefundBreakdown breakdown = SurrenderRefundBreakdown.of(mutable, BigDecimal.ZERO);

        mutable.add(debit(SurrenderFactorType.HANDLING_FEE, "50.00"));

        assertEquals(1, breakdown.factors().size());
        assertThrows(UnsupportedOperationException.class, () -> breakdown.factors().add(mutable.get(0)));
    }

    @Test
    @DisplayName("客户保留金额为负即拒绝")
    void shouldRejectNegativeRetainedAmount() {
        assertThrows(PricingDomainException.class, () -> SurrenderRefundBreakdown.of(
                List.of(credit(SurrenderFactorType.CASH_VALUE, "1000.00")), new BigDecimal("-0.01")));
    }

    private static SurrenderFactor credit(SurrenderFactorType type, String amount) {
        return new SurrenderFactor(type, ChargeDirection.CREDIT, new BigDecimal(amount), keyOf(type));
    }

    private static SurrenderFactor debit(SurrenderFactorType type, String amount) {
        return new SurrenderFactor(type, ChargeDirection.DEBIT, new BigDecimal(amount), keyOf(type));
    }

    private static SurrenderFactor none(SurrenderFactorType type) {
        return new SurrenderFactor(type, ChargeDirection.NONE, BigDecimal.ZERO, keyOf(type));
    }

    private static String keyOf(SurrenderFactorType type) {
        return "surrender.factor." + type.name().toLowerCase();
    }
}
