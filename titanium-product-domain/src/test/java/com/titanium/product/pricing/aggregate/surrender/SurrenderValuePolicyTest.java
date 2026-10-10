package com.titanium.product.pricing.aggregate.surrender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.SurrenderFactorType;
import com.titanium.product.common.enums.SurrenderRefundBasis;
import com.titanium.product.common.enums.SurrenderRefundType;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.pricing.premium.SurrenderBalanceFacts;
import com.titanium.product.valueobject.pricing.premium.SurrenderFactor;
import com.titanium.product.valueobject.pricing.premium.SurrenderLoanFacts;
import com.titanium.product.valueobject.pricing.premium.SurrenderPremiumFacts;
import com.titanium.product.valueobject.pricing.premium.SurrenderRefundBreakdown;
import com.titanium.product.valueobject.pricing.premium.SurrenderValueOutcome;

class SurrenderValuePolicyTest {

    private static final LocalDate EFFECTIVE_DATE = LocalDate.of(2026, 1, 1);
    private static final BigDecimal CUSTOMER_PAYABLE = new BigDecimal("121.20");

    /**
     * 「确定没有未结贷款」的业务事实：金额为 0。
     * <p>
     * 🔴 与 {@link SurrenderLoanFacts#UNAVAILABLE}（取不到）严格区分：前者是 CREDIT/DEBIT 的 0，
     * 后者在使用点抛 {@code SURRENDER_LOAN_FACTS_MISSING} 拒算。
     * </p>
     */
    private static final SurrenderLoanFacts NO_LOAN =
            new SurrenderLoanFacts(BigDecimal.ZERO, BigDecimal.ZERO);

    /**
     * 「确定没有欠费、没有红利、没有投资账户」的业务事实：三项均为 0。
     * <p>
     * 🔴 与 {@link SurrenderBalanceFacts#UNAVAILABLE}（取不到）严格区分：前者是 DEBIT/0 与 CREDIT/0，
     * 后者在使用点抛 {@code SURRENDER_BALANCE_FACTS_MISSING} 拒算。
     * </p>
     */
    private static final SurrenderBalanceFacts NO_BALANCE =
            new SurrenderBalanceFacts(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    @Test
    void shouldRefundFullCustomerPayableOnLastCoolingOffDay() {
        SurrenderValuePolicy policy = publishedPolicy();
        LocalDate effectiveDate = LocalDate.of(2026, 8, 1);

        SurrenderValueOutcome outcome = policy.calculate(
                new BigDecimal("121.20"), effectiveDate, effectiveDate.plusDays(15),
                SurrenderPremiumFacts.UNAVAILABLE, NO_LOAN, NO_BALANCE, 2, RoundingMode.HALF_UP);

        assertTrue(outcome.withinCoolingOff());
        assertEquals(SurrenderRefundType.COOLING_OFF_FULL_REFUND, outcome.refundType());
        assertEquals(new BigDecimal("121.20"), outcome.refundAmount());
        assertEquals(new BigDecimal("0.00"), outcome.retainedCustomerAmount());
    }

    @Test
    void shouldApplyPublishedCashValueRateOutsideCoolingOff() {
        SurrenderValuePolicy policy = publishedPolicy();

        SurrenderValueOutcome outcome = policy.calculate(
                CUSTOMER_PAYABLE, EFFECTIVE_DATE, LocalDate.of(2026, 8, 20),
                SurrenderPremiumFacts.UNAVAILABLE, NO_LOAN, NO_BALANCE, 2, RoundingMode.HALF_UP);

        assertFalse(outcome.withinCoolingOff());
        assertEquals(SurrenderRefundType.CASH_VALUE, outcome.refundType());
        assertEquals(new BigDecimal("72.72"), outcome.refundAmount());
        assertEquals(new BigDecimal("48.48"), outcome.retainedCustomerAmount());
        assertEquals(0, outcome.internalCostRetentionRate().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("犹豫期内：明细为「现金价值」全额加项 + 未满期保费不适用位，文案键指向全额退")
    void shouldCarryFullRefundFactorWithinCoolingOff() {
        LocalDate effectiveDate = LocalDate.of(2026, 8, 1);
        SurrenderValueOutcome outcome = publishedPolicy().calculate(
                new BigDecimal("121.20"), effectiveDate, effectiveDate.plusDays(15),
                SurrenderPremiumFacts.UNAVAILABLE, NO_LOAN, NO_BALANCE, 2, RoundingMode.HALF_UP);

        List<SurrenderFactor> factors = outcome.breakdown().factors();
        assertEquals(6, factors.size(), "六个退费因子恒在明细中（基准择一 + 未满期 + 贷款本息 + 欠缴保费 + 红利及账户价值 + 退保手续费），"
                + "未生效者以 NONE/0 占位");
        assertEquals(SurrenderFactorType.CASH_VALUE, factors.get(0).factorType());
        assertEquals(ChargeDirection.CREDIT, factors.get(0).direction());
        assertEquals(SurrenderValueOutcome.EXPLAIN_COOLING_OFF_FULL_REFUND, factors.get(0).explainKey());
        assertUnapplied(factors.get(1), SurrenderFactorType.UNEARNED_PREMIUM);
        assertAmountEquals(outcome.refundAmount(), outcome.breakdown().netAmount());
        assertAmountEquals(outcome.retainedCustomerAmount(), outcome.breakdown().retainedCustomerAmount());
    }

    @Test
    @DisplayName("现金价值基准：明细文案键指向现金价值，未满期保费因子为 NONE/0（本产品不适用）")
    void shouldCarryCashValueFactorOutsideCoolingOff() {
        SurrenderValueOutcome outcome = publishedPolicy().calculate(
                CUSTOMER_PAYABLE, EFFECTIVE_DATE, LocalDate.of(2026, 8, 20),
                SurrenderPremiumFacts.UNAVAILABLE, NO_LOAN, NO_BALANCE, 2, RoundingMode.HALF_UP);

        List<SurrenderFactor> factors = outcome.breakdown().factors();
        assertEquals(6, factors.size());
        assertEquals(SurrenderValueOutcome.EXPLAIN_CASH_VALUE, factors.get(0).explainKey());
        assertEquals(0, factors.get(0).amount().compareTo(new BigDecimal("72.72")),
                "因子金额须与应退金额一致：新口径下退款金额是因子加总的产物，不再是独立算出的数");
        assertUnapplied(factors.get(1), SurrenderFactorType.UNEARNED_PREMIUM);
        assertAmountEquals(outcome.refundAmount(), outcome.breakdown().netAmount());
    }

    @Test
    @DisplayName("明细毛额恰为应退金额，且无未结贷款时扣项合计为 0")
    void shouldHaveNoDeductionsUnderSingleFactorBaseline() {
        SurrenderValueOutcome outcome = publishedPolicy().calculate(
                CUSTOMER_PAYABLE, EFFECTIVE_DATE, LocalDate.of(2026, 8, 20),
                SurrenderPremiumFacts.UNAVAILABLE, NO_LOAN, NO_BALANCE, 2, RoundingMode.HALF_UP);

        assertAmountEquals(new BigDecimal("72.72"), outcome.breakdown().grossAmount());
        assertAmountEquals(BigDecimal.ZERO, outcome.breakdown().deductions());
        assertAmountEquals(BigDecimal.ZERO, outcome.breakdown().feeAmount());
    }

    // ==================== g05-02 未满期保费基准 ====================

    @Test
    @DisplayName("AC-04 ①：未满期保费 = 年保费 × 剩余保障天数 / 365（按日比例）")
    void shouldRefundUnearnedPremiumByDailyProportion() {
        LocalDate surrenderDate = LocalDate.of(2026, 3, 1);
        SurrenderPremiumFacts facts = new SurrenderPremiumFacts(
                new BigDecimal("3650.00"), surrenderDate.plusDays(200), false);

        SurrenderValueOutcome outcome = unearnedPolicy().calculate(
                new BigDecimal("4000.00"), EFFECTIVE_DATE, surrenderDate, facts, NO_LOAN, NO_BALANCE, 2, RoundingMode.HALF_UP);

        // 3650 × 200 / 365 = 2000.00
        assertEquals(SurrenderRefundType.UNEARNED_PREMIUM, outcome.refundType());
        assertFalse(outcome.withinCoolingOff());
        assertAmountEquals(new BigDecimal("2000.00"), outcome.refundAmount());
        assertAmountEquals(new BigDecimal("2000.00"), outcome.retainedCustomerAmount());
        assertEquals(0, outcome.cashValueRate().compareTo(BigDecimal.ZERO),
                "未满期保费基准不适用费率，报 0 而非沿用现金价值率");
    }

    @Test
    @DisplayName("AC-04 ①：未满期保费基准下现金价值因子为 NONE/0，明细中两个因子恒在（基准择一）")
    void shouldKeepBothFactorsUnderUnearnedPremiumBasis() {
        LocalDate surrenderDate = LocalDate.of(2026, 3, 1);
        SurrenderPremiumFacts facts = new SurrenderPremiumFacts(
                new BigDecimal("3650.00"), surrenderDate.plusDays(200), false);

        List<SurrenderFactor> factors = unearnedPolicy()
                .calculate(new BigDecimal("4000.00"), EFFECTIVE_DATE, surrenderDate, facts, NO_LOAN, NO_BALANCE, 2, RoundingMode.HALF_UP)
                .breakdown().factors();

        assertEquals(6, factors.size());
        assertUnapplied(factors.get(0), SurrenderFactorType.CASH_VALUE);
        assertEquals(SurrenderFactorType.UNEARNED_PREMIUM, factors.get(1).factorType());
        assertEquals(ChargeDirection.CREDIT, factors.get(1).direction());
        assertEquals(SurrenderValueOutcome.EXPLAIN_UNEARNED_PREMIUM, factors.get(1).explainKey());
        assertAmountEquals(new BigDecimal("2000.00"), factors.get(1).amount());
    }

    @Test
    @DisplayName("AC-04 ③：剩余天数超过一年时按 365 截断，未满期保费不超过年保费自身")
    void shouldCapRemainingDaysAtOneYear() {
        LocalDate surrenderDate = LocalDate.of(2026, 3, 1);
        SurrenderPremiumFacts facts = new SurrenderPremiumFacts(
                new BigDecimal("4000.00"), surrenderDate.plusDays(400), false);

        SurrenderValueOutcome outcome = unearnedPolicy().calculate(
                new BigDecimal("4000.00"), EFFECTIVE_DATE, surrenderDate, facts, NO_LOAN, NO_BALANCE, 2, RoundingMode.HALF_UP);

        assertAmountEquals(new BigDecimal("4000.00"), outcome.refundAmount());
        assertAmountEquals(new BigDecimal("0.00"), outcome.retainedCustomerAmount());
    }

    @Test
    @DisplayName("AC-04 ③：保障期已过（剩余天数为负）按 0 截断，不出现负额退款")
    void shouldFloorRemainingDaysAtZero() {
        LocalDate surrenderDate = LocalDate.of(2026, 3, 1);
        SurrenderPremiumFacts facts = new SurrenderPremiumFacts(
                new BigDecimal("4000.00"), surrenderDate.minusDays(30), false);

        SurrenderValueOutcome outcome = unearnedPolicy().calculate(
                new BigDecimal("4000.00"), EFFECTIVE_DATE, surrenderDate, facts, NO_LOAN, NO_BALANCE, 2, RoundingMode.HALF_UP);

        assertAmountEquals(BigDecimal.ZERO, outcome.refundAmount());
        assertAmountEquals(new BigDecimal("4000.00"), outcome.retainedCustomerAmount());
        assertAmountEquals(BigDecimal.ZERO, outcome.breakdown().netAmount());
    }

    @Test
    @DisplayName("AC-04 ②：缴费期满（含趸缴）该项为 CREDIT/0 —— 「适用但为 0」，不是缺项")
    void shouldReportPaidUpPremiumAsAppliedZeroRatherThanMissing() {
        LocalDate surrenderDate = LocalDate.of(2026, 3, 1);
        SurrenderPremiumFacts paidUp = new SurrenderPremiumFacts(null, null, true);

        SurrenderValueOutcome outcome = unearnedPolicy().calculate(
                new BigDecimal("4000.00"), EFFECTIVE_DATE, surrenderDate, paidUp, NO_LOAN, NO_BALANCE,
                2, RoundingMode.HALF_UP);

        List<SurrenderFactor> factors = outcome.breakdown().factors();
        assertEquals(6, factors.size(), "缴费期满只让金额为 0，不得让因子从明细中消失");
        SurrenderFactor unearned = factors.get(1);
        assertEquals(SurrenderFactorType.UNEARNED_PREMIUM, unearned.factorType());
        assertEquals(ChargeDirection.CREDIT, unearned.direction(),
                "缴费期满与「本产品不适用」必须可区分：前者为 CREDIT/0，后者为 NONE/0");
        assertAmountEquals(BigDecimal.ZERO, unearned.amount());
        assertAmountEquals(BigDecimal.ZERO, outcome.refundAmount());
        assertAmountEquals(new BigDecimal("4000.00"), outcome.retainedCustomerAmount());
    }

    @Test
    @DisplayName("保费事实缺失时失败关闭，不以 0 兜底（「取不到」≠「确定为 0」）")
    void shouldFailClosedWhenPremiumFactsMissing() {
        LocalDate surrenderDate = LocalDate.of(2026, 3, 1);
        SurrenderValuePolicy policy = unearnedPolicy();

        PricingDomainException absent = assertThrows(PricingDomainException.class,
                () -> policy.calculate(new BigDecimal("4000.00"), EFFECTIVE_DATE, surrenderDate,
                        SurrenderPremiumFacts.UNAVAILABLE, NO_LOAN, NO_BALANCE, 2, RoundingMode.HALF_UP));
        assertEquals(ProductErrorCode.SURRENDER_PREMIUM_FACTS_MISSING, absent.getErrorCode());

        SurrenderPremiumFacts noCoverageEnd = new SurrenderPremiumFacts(
                new BigDecimal("3650.00"), null, false);
        PricingDomainException partial = assertThrows(PricingDomainException.class,
                () -> policy.calculate(new BigDecimal("4000.00"), EFFECTIVE_DATE, surrenderDate,
                        noCoverageEnd, NO_LOAN, NO_BALANCE, 2, RoundingMode.HALF_UP));
        assertEquals(ProductErrorCode.SURRENDER_PREMIUM_FACTS_MISSING, partial.getErrorCode());
    }

    @Test
    @DisplayName("应退金额超过客户应付即拒绝：客户保留金额不得为负（退得比交的多）")
    void shouldRejectRefundExceedingCustomerPayable() {
        LocalDate surrenderDate = LocalDate.of(2026, 3, 1);
        SurrenderPremiumFacts facts = new SurrenderPremiumFacts(
                new BigDecimal("4000.00"), surrenderDate.plusDays(365), false);

        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> unearnedPolicy().calculate(new BigDecimal("1000.00"), EFFECTIVE_DATE, surrenderDate,
                        facts, NO_LOAN, NO_BALANCE, 2, RoundingMode.HALF_UP));

        assertEquals(ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("客户保留金额为负"));
    }

    @Test
    @DisplayName("现金价值口径入口不提供保费事实时，未满期因子同样以 NONE/0 占位")
    void shouldKeepUnearnedFactorUnappliedInCashValueOnlyEntry() {
        List<SurrenderFactor> factors = publishedPolicy()
                .cashValueAt(CUSTOMER_PAYABLE, EFFECTIVE_DATE, LocalDate.of(2026, 8, 20), 2, RoundingMode.HALF_UP)
                .breakdown().factors();

        assertEquals(6, factors.size());
        assertAmountEquals(new BigDecimal("72.72"), factors.get(0).amount());
        assertUnapplied(factors.get(1), SurrenderFactorType.UNEARNED_PREMIUM);
    }

    @Test
    @DisplayName("内容哈希纳入退费基准：两个基准的同一策略内容哈希不同")
    void shouldIncludeRefundBasisInContentHash() {
        assertFalse(publishedPolicy(SurrenderRefundBasis.CASH_VALUE).getContentHash()
                        .equals(publishedPolicy(SurrenderRefundBasis.UNEARNED_PREMIUM).getContentHash()),
                "退费基准改变应退金额口径，必须进入内容哈希，否则策略变更无法取证");
    }

    @Test
    @DisplayName("应退金额与明细净额漂移即拒绝：结果对象不得脱离明细自报金额")
    void shouldRejectOutcomeAmountDriftedFromBreakdown() {
        SurrenderRefundBreakdown breakdown = SurrenderRefundBreakdown.of(
                List.of(new SurrenderFactor(SurrenderFactorType.CASH_VALUE, ChargeDirection.CREDIT,
                        new BigDecimal("72.72"), SurrenderValueOutcome.EXPLAIN_CASH_VALUE)),
                new BigDecimal("48.48"));

        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> new SurrenderValueOutcome(SurrenderRefundType.CASH_VALUE, false, new BigDecimal("0.60"),
                        new BigDecimal("72.73"), new BigDecimal("48.48"), BigDecimal.ZERO, breakdown));

        assertEquals(ProductErrorCode.SURRENDER_FACTOR_INVALID, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("应退金额"));
    }

    @Test
    @DisplayName("兼容构造器：无因子语境时按「现金价值」单因子补齐明细")
    void shouldBackfillBreakdownInCompatibilityConstructor() {
        SurrenderValueOutcome outcome = new SurrenderValueOutcome(SurrenderRefundType.CASH_VALUE, false,
                new BigDecimal("0.60000000"), new BigDecimal("72.72"), new BigDecimal("48.48"),
                BigDecimal.ZERO);

        assertEquals(1, outcome.breakdown().factors().size());
        assertEquals(SurrenderValueOutcome.EXPLAIN_CASH_VALUE, outcome.breakdown().factors().get(0).explainKey());
        assertAmountEquals(outcome.refundAmount(), outcome.breakdown().netAmount());
    }

    // ==================== g05-03 保单贷款未偿本息 ====================

    @Test
    @DisplayName("AC-05 ①：无未结贷款是 DEBIT/0——「适用但为 0」，不是 NONE（本产品不适用），应退金额逐字不变")
    void shouldReportNoOutstandingLoanAsAppliedZeroRatherThanUnapplied() {
        LocalDate surrenderDate = LocalDate.of(2026, 3, 1);
        SurrenderPremiumFacts facts = new SurrenderPremiumFacts(
                new BigDecimal("3650.00"), surrenderDate.plusDays(200), false);

        SurrenderValueOutcome outcome = unearnedPolicy().calculate(
                new BigDecimal("4000.00"), EFFECTIVE_DATE, surrenderDate, facts, NO_LOAN, NO_BALANCE, 2, RoundingMode.HALF_UP);

        SurrenderFactor loan = outcome.breakdown().factors().get(2);
        assertEquals(SurrenderFactorType.LOAN_PRINCIPAL_INTEREST, loan.factorType());
        assertEquals(ChargeDirection.DEBIT, loan.direction(),
                "零贷款与「本产品不适用」必须可区分：前者为 DEBIT/0，后者为 NONE/0");
        assertAmountEquals(BigDecimal.ZERO, loan.amount());
        assertAmountEquals(BigDecimal.ZERO, outcome.breakdown().deductions());
        assertAmountEquals(new BigDecimal("2000.00"), outcome.refundAmount(),
                "零扣项不得改变应退金额：无贷款保单升级后金额与请求哈希均逐字不变");
    }

    @Test
    @DisplayName("AC-05 ②：未满期保费基准下先行抵扣贷款未偿本息（本金 1000 + 利息 50）")
    void shouldDeductOutstandingLoanFromUnearnedPremiumRefund() {
        LocalDate surrenderDate = LocalDate.of(2026, 3, 1);
        SurrenderPremiumFacts facts = new SurrenderPremiumFacts(
                new BigDecimal("3650.00"), surrenderDate.plusDays(200), false);
        SurrenderLoanFacts loan = new SurrenderLoanFacts(
                new BigDecimal("1000.00"), new BigDecimal("50.00"));

        SurrenderValueOutcome outcome = unearnedPolicy().calculate(
                new BigDecimal("4000.00"), EFFECTIVE_DATE, surrenderDate, facts, loan, NO_BALANCE, 2, RoundingMode.HALF_UP);

        SurrenderFactor factor = outcome.breakdown().factors().get(2);
        assertEquals(SurrenderFactorType.LOAN_PRINCIPAL_INTEREST, factor.factorType());
        assertEquals(ChargeDirection.DEBIT, factor.direction());
        assertEquals(SurrenderValueOutcome.EXPLAIN_LOAN_PRINCIPAL_INTEREST, factor.explainKey());
        assertAmountEquals(new BigDecimal("1050.00"), factor.amount());
        assertAmountEquals(new BigDecimal("1050.00"), outcome.breakdown().deductions());
        assertAmountEquals(new BigDecimal("950.00"), outcome.refundAmount()); // 2000 − 1050
        assertAmountEquals(new BigDecimal("3050.00"), outcome.retainedCustomerAmount());
        assertAmountEquals(outcome.refundAmount(), outcome.breakdown().netAmount());
    }

    @Test
    @DisplayName("贷款抵扣与退费基准无关：犹豫期全额退同样先扣未偿本息")
    void shouldDeductOutstandingLoanFromCoolingOffFullRefund() {
        LocalDate effectiveDate = LocalDate.of(2026, 8, 1);
        SurrenderLoanFacts loan = new SurrenderLoanFacts(
                new BigDecimal("20.00"), new BigDecimal("1.20"));

        SurrenderValueOutcome outcome = publishedPolicy().calculate(
                new BigDecimal("121.20"), effectiveDate, effectiveDate.plusDays(15),
                SurrenderPremiumFacts.UNAVAILABLE, loan, NO_BALANCE, 2, RoundingMode.HALF_UP);

        assertTrue(outcome.withinCoolingOff());
        assertAmountEquals(new BigDecimal("100.00"), outcome.refundAmount()); // 121.20 − 21.20
        assertAmountEquals(new BigDecimal("21.20"), outcome.retainedCustomerAmount());
    }

    @Test
    @DisplayName("AC-05 ③：贷款事实取不到即失败关闭，绝不按「无贷款」以 0 抵扣")
    void shouldFailClosedWhenLoanFactsMissing() {
        LocalDate surrenderDate = LocalDate.of(2026, 3, 1);
        SurrenderPremiumFacts facts = new SurrenderPremiumFacts(
                new BigDecimal("3650.00"), surrenderDate.plusDays(200), false);
        SurrenderValuePolicy policy = unearnedPolicy();

        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> policy.calculate(new BigDecimal("4000.00"), EFFECTIVE_DATE, surrenderDate,
                        facts, SurrenderLoanFacts.UNAVAILABLE, NO_BALANCE, 2, RoundingMode.HALF_UP));

        assertEquals(ProductErrorCode.SURRENDER_LOAN_FACTS_MISSING, ex.getErrorCode());
    }

    @Test
    @DisplayName("贷款未偿本金与利息必须成对下发：只给一项在构造期即拒，不进计算")
    void shouldRejectUnpairedLoanFacts() {
        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> new SurrenderLoanFacts(new BigDecimal("1000.00"), null));

        assertEquals(ProductErrorCode.SURRENDER_FACTOR_INVALID, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("成对"));
    }

    @Test
    @DisplayName("贷款未偿本息不得为负：负额是取数错误，不得被当作贷方加项")
    void shouldRejectNegativeLoanAmount() {
        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> new SurrenderLoanFacts(new BigDecimal("-1.00"), BigDecimal.ZERO));

        assertEquals(ProductErrorCode.SURRENDER_FACTOR_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("未偿本息超过应退金额即拒绝：净额为负，须转人工核价而非自动出账")
    void shouldRejectLoanExceedingRefundAmount() {
        LocalDate surrenderDate = LocalDate.of(2026, 3, 1);
        SurrenderPremiumFacts facts = new SurrenderPremiumFacts(
                new BigDecimal("3650.00"), surrenderDate.plusDays(200), false);
        SurrenderLoanFacts loan = new SurrenderLoanFacts(new BigDecimal("3000.00"), BigDecimal.ZERO);

        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> unearnedPolicy().calculate(new BigDecimal("4000.00"), EFFECTIVE_DATE, surrenderDate,
                        facts, loan, NO_BALANCE, 2, RoundingMode.HALF_UP));

        assertEquals(ProductErrorCode.SURRENDER_NET_AMOUNT_NEGATIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("现金价值只读入口不产生任何退款：贷款因子恒为 NONE/0（不适用），不因取不到而拒绝")
    void shouldKeepLoanFactorUnappliedInCashValueOnlyEntry() {
        List<SurrenderFactor> factors = publishedPolicy()
                .cashValueAt(CUSTOMER_PAYABLE, EFFECTIVE_DATE, LocalDate.of(2026, 8, 20), 2, RoundingMode.HALF_UP)
                .breakdown().factors();

        assertUnapplied(factors.get(2), SurrenderFactorType.LOAN_PRINCIPAL_INTEREST);
    }

    // ==================== g05-04 欠缴保费与红利/账户价值 ====================

    @Test
    @DisplayName("AC-05 ①：无欠费是 DEBIT/0、无红利与投资账户是 CREDIT/0 —— 「适用但为 0」，不是 NONE")
    void shouldReportNoBalanceAsAppliedZeroRatherThanUnapplied() {
        LocalDate surrenderDate = LocalDate.of(2026, 3, 1);
        SurrenderPremiumFacts facts = new SurrenderPremiumFacts(
                new BigDecimal("3650.00"), surrenderDate.plusDays(200), false);

        SurrenderValueOutcome outcome = unearnedPolicy().calculate(
                new BigDecimal("4000.00"), EFFECTIVE_DATE, surrenderDate, facts, NO_LOAN, NO_BALANCE,
                2, RoundingMode.HALF_UP);

        SurrenderFactor overdue = outcome.breakdown().factors().get(3);
        assertEquals(SurrenderFactorType.OVERDUE_PREMIUM, overdue.factorType());
        assertEquals(ChargeDirection.DEBIT, overdue.direction(),
                "零欠费与「本产品不适用」必须可区分：前者为 DEBIT/0，后者为 NONE/0");
        assertAmountEquals(BigDecimal.ZERO, overdue.amount());
        assertEquals(SurrenderValueOutcome.EXPLAIN_OVERDUE_PREMIUM, overdue.explainKey());

        SurrenderFactor dividendAccount = outcome.breakdown().factors().get(4);
        assertEquals(SurrenderFactorType.DIVIDEND_ACCOUNT_VALUE, dividendAccount.factorType());
        assertEquals(ChargeDirection.CREDIT, dividendAccount.direction(),
                "零余额与「本产品不适用」必须可区分：前者为 CREDIT/0，后者为 NONE/0");
        assertAmountEquals(BigDecimal.ZERO, dividendAccount.amount());

        assertAmountEquals(BigDecimal.ZERO, outcome.breakdown().deductions());
        assertAmountEquals(new BigDecimal("2000.00"), outcome.refundAmount(),
                "零欠费零余额不得改变应退金额：无欠费无权益保单升级后金额与请求哈希均逐字不变");
    }

    @Test
    @DisplayName("AC-05 ②：欠缴保费先行抵扣、红利与投资账户价值一并退还，且两者在本域合并为一个因子金额")
    void shouldDeductArrearsAndRefundDividendAccountValue() {
        LocalDate surrenderDate = LocalDate.of(2026, 3, 1);
        SurrenderPremiumFacts facts = new SurrenderPremiumFacts(
                new BigDecimal("3650.00"), surrenderDate.plusDays(200), false);
        // 欠费 300 先行抵扣；红利 500 + 投资账户价值 200 = 700 一并退还
        SurrenderBalanceFacts balance = new SurrenderBalanceFacts(
                new BigDecimal("300.00"), new BigDecimal("500.00"), new BigDecimal("200.00"));

        SurrenderValueOutcome outcome = unearnedPolicy().calculate(
                new BigDecimal("4000.00"), EFFECTIVE_DATE, surrenderDate, facts, NO_LOAN, balance,
                2, RoundingMode.HALF_UP);

        SurrenderFactor overdue = outcome.breakdown().factors().get(3);
        assertEquals(ChargeDirection.DEBIT, overdue.direction());
        assertEquals(SurrenderValueOutcome.EXPLAIN_OVERDUE_PREMIUM, overdue.explainKey());
        assertAmountEquals(new BigDecimal("300.00"), overdue.amount());

        SurrenderFactor dividendAccount = outcome.breakdown().factors().get(4);
        assertEquals(ChargeDirection.CREDIT, dividendAccount.direction());
        assertEquals(SurrenderValueOutcome.EXPLAIN_DIVIDEND_ACCOUNT_VALUE, dividendAccount.explainKey());
        assertAmountEquals(new BigDecimal("700.00"), dividendAccount.amount(),
                "红利与投资账户价值合并为「红利及账户价值」一个因子：500 + 200");

        assertAmountEquals(new BigDecimal("2700.00"), outcome.breakdown().grossAmount()); // 2000 + 700
        assertAmountEquals(new BigDecimal("300.00"), outcome.breakdown().deductions());
        assertAmountEquals(new BigDecimal("2400.00"), outcome.refundAmount()); // 2000 − 300 + 700
        assertAmountEquals(new BigDecimal("1600.00"), outcome.retainedCustomerAmount()); // 4000 − 2400
        assertAmountEquals(outcome.refundAmount(), outcome.breakdown().netAmount());
    }

    @Test
    @DisplayName("AC-05 ③：账户余额事实取不到即失败关闭，绝不按「无欠费、无余额」以 0 兜底")
    void shouldFailClosedWhenBalanceFactsMissing() {
        LocalDate surrenderDate = LocalDate.of(2026, 3, 1);
        SurrenderPremiumFacts facts = new SurrenderPremiumFacts(
                new BigDecimal("3650.00"), surrenderDate.plusDays(200), false);

        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> unearnedPolicy().calculate(new BigDecimal("4000.00"), EFFECTIVE_DATE, surrenderDate,
                        facts, NO_LOAN, SurrenderBalanceFacts.UNAVAILABLE, 2, RoundingMode.HALF_UP));

        assertEquals(ProductErrorCode.SURRENDER_BALANCE_FACTS_MISSING, ex.getErrorCode());
    }

    @Test
    @DisplayName("欠缴保费与红利/账户价值必须同批下发：只给一项在构造期即拒，不进计算")
    void shouldRejectPartiallyProvidedBalanceFacts() {
        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> new SurrenderBalanceFacts(BigDecimal.ZERO, null, null));

        assertEquals(ProductErrorCode.SURRENDER_FACTOR_INVALID, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("同批"));
    }

    @Test
    @DisplayName("欠费与账户余额不得为负：负额是取数错误，不得被当作贷方加项")
    void shouldRejectNegativeBalanceAmount() {
        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> new SurrenderBalanceFacts(new BigDecimal("-1.00"), BigDecimal.ZERO, BigDecimal.ZERO));

        assertEquals(ProductErrorCode.SURRENDER_FACTOR_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("现金价值只读入口不产生任何退款：余额两因子恒为 NONE/0（不适用），不因取不到而拒绝")
    void shouldKeepBalanceFactorsUnappliedInCashValueOnlyEntry() {
        List<SurrenderFactor> factors = publishedPolicy()
                .cashValueAt(CUSTOMER_PAYABLE, EFFECTIVE_DATE, LocalDate.of(2026, 8, 20), 2, RoundingMode.HALF_UP)
                .breakdown().factors();

        assertUnapplied(factors.get(3), SurrenderFactorType.OVERDUE_PREMIUM);
        assertUnapplied(factors.get(4), SurrenderFactorType.DIVIDEND_ACCOUNT_VALUE);
    }

    @Test
    @DisplayName("犹豫期内全额退叠加非零红利即触发「客户保留金额为负」守卫：结构性可达，须转人工核价（R-7 待裁示）")
    void shouldRejectDividendRefundExceedingCustomerPayableWithinCoolingOff() {
        LocalDate effectiveDate = LocalDate.of(2026, 8, 1);
        // 犹豫期全额退 121.20，叠加红利 100.00 ⇒ 净额 221.20 > 客户实付 121.20
        SurrenderBalanceFacts balance = new SurrenderBalanceFacts(
                BigDecimal.ZERO, new BigDecimal("100.00"), BigDecimal.ZERO);

        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> publishedPolicy().calculate(new BigDecimal("121.20"), effectiveDate,
                        effectiveDate.plusDays(15), SurrenderPremiumFacts.UNAVAILABLE, NO_LOAN,
                        balance, 2, RoundingMode.HALF_UP));

        assertEquals(ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("客户保留金额为负"),
                "守卫文案须可执行：此处不是「净额为负」（扣项过多），而是「退得比交的多」");
    }

    @Test
    @DisplayName("g05-05 ①：只读现金价值入口不产生退款——已配置手续费率也不扣，因子为 NONE/0 且金额不变")
    void shouldKeepHandlingFeeUnappliedInCashValueOnlyEntry() {
        SurrenderValueOutcome outcome = publishedPolicy(SurrenderRefundBasis.CASH_VALUE, new BigDecimal("0.10"))
                .cashValueAt(CUSTOMER_PAYABLE, EFFECTIVE_DATE, LocalDate.of(2026, 8, 20), 2, RoundingMode.HALF_UP);

        assertUnapplied(outcome.breakdown().factors().get(5), SurrenderFactorType.HANDLING_FEE);
        // 121.20 × 0.6 = 72.72：手续费率配置不得影响只读现金价值口径
        assertAmountEquals(new BigDecimal("72.72"), outcome.refundAmount(),
                "只读口径不产生退款指令，故手续费既不适用也不得扣减现金价值");
    }

    @Test
    @DisplayName("g05-05 ②：退费语境但产品未配置手续费率 ⇒ NONE/0「不适用」，净额与无该因子时逐分相同")
    void shouldReportHandlingFeeUnappliedWhenRateAbsent() {
        SurrenderValueOutcome outcome = publishedPolicy(SurrenderRefundBasis.CASH_VALUE, null).calculate(
                CUSTOMER_PAYABLE, EFFECTIVE_DATE, LocalDate.of(2026, 8, 20), SurrenderPremiumFacts.UNAVAILABLE,
                NO_LOAN, NO_BALANCE, 2, RoundingMode.HALF_UP);

        assertUnapplied(outcome.breakdown().factors().get(5), SurrenderFactorType.HANDLING_FEE);
        assertAmountEquals(BigDecimal.ZERO, outcome.breakdown().feeAmount());
        assertAmountEquals(new BigDecimal("72.72"), outcome.breakdown().netAmount());
    }

    @Test
    @DisplayName("g05-05 ③：犹豫期内不扣退保手续费——已配置费率也报 DEBIT/0「适用但为 0」，不得退化成不适用")
    void shouldReportZeroHandlingFeeWithinCoolingOff() {
        LocalDate effectiveDate = LocalDate.of(2026, 8, 1);

        List<SurrenderFactor> factors = publishedPolicy(SurrenderRefundBasis.CASH_VALUE, new BigDecimal("0.10"))
                .calculate(new BigDecimal("121.20"), effectiveDate, effectiveDate.plusDays(15),
                        SurrenderPremiumFacts.UNAVAILABLE, NO_LOAN, NO_BALANCE, 2, RoundingMode.HALF_UP)
                .breakdown().factors();

        SurrenderFactor fee = factors.get(5);
        assertEquals(SurrenderFactorType.HANDLING_FEE, fee.factorType());
        assertEquals(ChargeDirection.DEBIT, fee.direction(),
                "犹豫期不扣手续费是「适用但为 0」，与未配置费率的「不适用」必须可区分（R-2 裁示）");
        assertAmountEquals(BigDecimal.ZERO, fee.amount());
    }

    @Test
    @DisplayName("g05-05 ④：犹豫期外按「应退基准金额 × 手续费率」实扣，手续费合计随明细透出")
    void shouldDeductHandlingFeeOnRefundBaseOutsideCoolingOff() {
        SurrenderValueOutcome outcome = publishedPolicy(SurrenderRefundBasis.CASH_VALUE, new BigDecimal("0.10"))
                .calculate(CUSTOMER_PAYABLE, EFFECTIVE_DATE, LocalDate.of(2026, 8, 20),
                        SurrenderPremiumFacts.UNAVAILABLE, NO_LOAN, NO_BALANCE, 2, RoundingMode.HALF_UP);

        SurrenderFactor fee = outcome.breakdown().factors().get(5);
        assertEquals(ChargeDirection.DEBIT, fee.direction());
        // 应退基准 121.20 × 0.6 = 72.72，手续费 72.72 × 0.10 = 7.272 → 7.27
        assertAmountEquals(new BigDecimal("7.27"), fee.amount());
        assertAmountEquals(new BigDecimal("7.27"), outcome.breakdown().feeAmount(), "手续费合计须按因子透出");
        assertAmountEquals(new BigDecimal("65.45"), outcome.breakdown().netAmount());
        assertAmountEquals(new BigDecimal("65.45"), outcome.refundAmount());
    }

    @Test
    @DisplayName("g05-05 R-10：退保手续费使净额由正转负 ⇒ 必须拒绝并给明确错误码，不生成负额退款指令")
    void shouldRejectNegativeNetAmountWhenHandlingFeeTurnsItNegative() {
        LocalDate surrenderDate = LocalDate.of(2026, 8, 20);
        SurrenderLoanFacts loan = new SurrenderLoanFacts(new BigDecimal("60.00"), new BigDecimal("10.00"));

        // 对照组：同案无手续费 ⇒ 毛额 72.72 − 贷款 70.00 = 2.72，通过（证明拒绝确由手续费触发）
        SurrenderValueOutcome withoutFee = publishedPolicy(SurrenderRefundBasis.CASH_VALUE, null).calculate(
                CUSTOMER_PAYABLE, EFFECTIVE_DATE, surrenderDate, SurrenderPremiumFacts.UNAVAILABLE,
                loan, NO_BALANCE, 2, RoundingMode.HALF_UP);
        assertAmountEquals(new BigDecimal("2.72"), withoutFee.breakdown().netAmount());

        // 加手续费率 0.10 ⇒ 手续费 7.27，扣项合计 77.27 > 应退合计 72.72 ⇒ 净额 -4.55
        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> publishedPolicy(SurrenderRefundBasis.CASH_VALUE, new BigDecimal("0.10")).calculate(
                        CUSTOMER_PAYABLE, EFFECTIVE_DATE, surrenderDate, SurrenderPremiumFacts.UNAVAILABLE,
                        loan, NO_BALANCE, 2, RoundingMode.HALF_UP));

        assertEquals(ProductErrorCode.SURRENDER_NET_AMOUNT_NEGATIVE, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("净额为"),
                "文案须点明是「净额为负」（扣项过多），而非「客户保留金额为负」");
    }

    @Test
    @DisplayName("g05-05：内容哈希纳入退保手续费率——费率不同的同一策略内容哈希不同")
    void shouldIncludeHandlingFeeRateInContentHash() {
        assertFalse(publishedPolicy(SurrenderRefundBasis.CASH_VALUE, null).getContentHash()
                        .equals(publishedPolicy(SurrenderRefundBasis.CASH_VALUE, new BigDecimal("0.10"))
                                .getContentHash()),
                "手续费率改变应退金额，必须进入内容哈希，否则策略变更无法取证");
    }

    @Test
    @DisplayName("g05-05：手续费率越界（>1）在策略创建期即拒绝，文案点名是哪个费率")
    void shouldRejectHandlingFeeRateOutOfRange() {
        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> SurrenderValuePolicy.createDraft(
                        "policy-1", "product-1", "LIFE-SURRENDER-CASH-VALUE", "V1.0", 1, 15,
                        new BigDecimal("0.60000000"), BigDecimal.ZERO, new BigDecimal("1.01"),
                        SurrenderRefundBasis.CASH_VALUE, LocalDateTime.of(2026, 1, 1, 0, 0), null, "1"));

        assertEquals(ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("退保手续费率"), "文案须点名越界字段，避免与内留率混淆");
    }

    private static void assertUnapplied(SurrenderFactor factor, SurrenderFactorType expectedType) {
        assertEquals(expectedType, factor.factorType());
        assertEquals(ChargeDirection.NONE, factor.direction(), "未生效因子方向必须为 NONE，金额为 0");
        assertAmountEquals(BigDecimal.ZERO, factor.amount());
    }

    private static void assertAmountEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual),
                "期望 " + expected.toPlainString() + "，实际 " + actual.toPlainString());
    }

    private static void assertAmountEquals(BigDecimal expected, BigDecimal actual, String reason) {
        assertEquals(0, expected.compareTo(actual), reason
                + "（期望 " + expected.toPlainString() + "，实际 " + actual.toPlainString() + "）");
    }

    private SurrenderValuePolicy publishedPolicy() {
        return publishedPolicy(SurrenderRefundBasis.CASH_VALUE);
    }

    private SurrenderValuePolicy unearnedPolicy() {
        return publishedPolicy(SurrenderRefundBasis.UNEARNED_PREMIUM);
    }

    private SurrenderValuePolicy publishedPolicy(SurrenderRefundBasis refundBasis) {
        return publishedPolicy(refundBasis, null);
    }

    /** 可指定手续费率的已发布策略；{@code handlingFeeRate == null} 表示该产品不适用退保手续费。 */
    private SurrenderValuePolicy publishedPolicy(SurrenderRefundBasis refundBasis, BigDecimal handlingFeeRate) {
        SurrenderValuePolicy policy = SurrenderValuePolicy.createDraft(
                "policy-1", "product-1", "LIFE-SURRENDER-CASH-VALUE", "V1.0", 1, 15,
                new BigDecimal("0.60000000"), BigDecimal.ZERO, handlingFeeRate, refundBasis,
                LocalDateTime.of(2026, 1, 1, 0, 0), null, "1");
        policy.approve();
        policy.publish();
        return policy;
    }
}
