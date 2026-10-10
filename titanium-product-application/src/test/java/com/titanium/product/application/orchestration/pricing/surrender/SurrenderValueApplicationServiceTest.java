package com.titanium.product.application.orchestration.pricing.surrender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.application.model.pricing.surrender.SurrenderValueCalculationResult;
import com.titanium.product.application.orchestration.pricing.PricingEvidenceHasher;
import com.titanium.product.application.orchestration.pricing.lifecycle.PremiumLifecycleAdjustmentApplicationService;
import com.titanium.product.command.pricing.lifecycle.CreatePremiumLifecycleAdjustmentCommand;
import com.titanium.product.command.pricing.surrender.CalculateSurrenderValueCommand;
import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.common.enums.SurrenderFactorType;
import com.titanium.product.common.enums.SurrenderRefundBasis;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.pricing.aggregate.PremiumCalculation;
import com.titanium.product.pricing.aggregate.lifecycle.PremiumLifecycleAdjustment;
import com.titanium.product.pricing.aggregate.surrender.SurrenderValuePolicy;
import com.titanium.product.repository.PremiumCalculationRepository;
import com.titanium.product.repository.SurrenderValuePolicyRepository;
import com.titanium.product.valueobject.pricing.calculation.CalculationLine;
import com.titanium.product.valueobject.pricing.calculation.CalculationTotals;
import com.titanium.product.valueobject.pricing.premium.PremiumCalculationEvidence;
import com.titanium.product.valueobject.pricing.premium.SurrenderBalanceFacts;
import com.titanium.product.valueobject.pricing.premium.SurrenderFactor;
import com.titanium.product.valueobject.pricing.premium.SurrenderValueOutcome;

class SurrenderValueApplicationServiceTest {

    /** 现金价值基准、客户应付 121.20 × 费率 0.6 = 72.72：本类多数用例的期望应退金额。 */
    private static final BigDecimal CASH_VALUE_REFUND = new BigDecimal("72.72");

    /** 「无欠费、无红利、无投资账户」的业务事实：三项均为 0。 */
    private static final SurrenderBalanceFacts ZERO_BALANCE =
            new SurrenderBalanceFacts(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    private PremiumCalculationRepository calculationRepository;
    private SurrenderValuePolicyRepository policyRepository;
    private PremiumLifecycleAdjustmentApplicationService lifecycleService;

    private SurrenderValueApplicationService service;

    @BeforeEach
    void setUp() {
        calculationRepository = mock(PremiumCalculationRepository.class);
        policyRepository = mock(SurrenderValuePolicyRepository.class);
        lifecycleService = mock(PremiumLifecycleAdjustmentApplicationService.class);
        service = new SurrenderValueApplicationService(
                calculationRepository, policyRepository, lifecycleService, new PricingEvidenceHasher());
    }

    @Test
    void shouldCreateAuditableReplacementCalculationAndCreditAdjustment() {
        PremiumCalculation original = originalCalculation();
        SurrenderValuePolicy policy = publishedPolicy();
        PremiumLifecycleAdjustment adjustment = mock(PremiumLifecycleAdjustment.class);
        when(calculationRepository.findById("1", "calc-original")).thenReturn(Optional.of(original));
        when(policyRepository.findPublished("1", "product-1", 1, LocalDateTime.of(2026, 8, 20, 12, 0)))
                .thenReturn(Optional.of(policy));
        when(calculationRepository.findByIdempotencyKey(
                "1", "maintenance-1:replacement", PricingCalculationPurpose.MAINTENANCE))
                .thenReturn(Optional.empty());
        when(adjustment.getCustomerAmount()).thenReturn(new BigDecimal("72.72"));
        when(adjustment.getAdjustmentId()).thenReturn("adjustment-1");
        when(adjustment.getRequestHash()).thenReturn(hash('q'));
        when(adjustment.getOriginalResultHash()).thenReturn(hash('r'));
        when(adjustment.getReplacementResultHash()).thenReturn(hash('s'));
        when(lifecycleService.create(any())).thenReturn(adjustment);

        SurrenderValueCalculationResult result = service.calculate(new CalculateSurrenderValueCommand(
                "1", "maintenance-1", "policy-1", "issuance-biz-1", "calc-original", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 8, 20), 1, LocalDateTime.of(2026, 8, 20, 12, 0), "寿险退保",
                null, null, false, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        ArgumentCaptor<PremiumCalculation> replacement = ArgumentCaptor.forClass(PremiumCalculation.class);
        ArgumentCaptor<CreatePremiumLifecycleAdjustmentCommand> lifecycleCommand =
                ArgumentCaptor.forClass(CreatePremiumLifecycleAdjustmentCommand.class);
        verify(calculationRepository).save(replacement.capture());
        verify(lifecycleService).create(lifecycleCommand.capture());
        assertEquals(new BigDecimal("72.72"), result.refundAmount());
        assertEquals(new BigDecimal("48.48"), replacement.getValue().getTotalPremium());
        assertEquals("issuance-biz-1", replacement.getValue().getBizNo());
        assertEquals("issuance-biz-1", lifecycleCommand.getValue().bizNo());
        assertEquals(new BigDecimal("0.00"), replacement.getValue().getCalculationTotals().internalCostTotal());
        assertEquals("LIFE-SURRENDER-CASH-VALUE",
                replacement.getValue().getRequestSnapshot().get("surrenderPolicyCode"));
        assertEquals(hash('q'), result.requestHash());
        assertEquals("P1", result.pricingPlanVersion());
        assertEquals(hash('p'), result.pricingPlanContentHash());
    }

    @Test
    @DisplayName("结果透出退费因子明细：跨域契约所需的类型码 / 方向码 / 金额 / 文案键齐备")
    void shouldExposeSurrenderFactorDetail() {
        PremiumCalculation original = originalCalculation();
        PremiumLifecycleAdjustment adjustment = mock(PremiumLifecycleAdjustment.class);
        when(calculationRepository.findById("1", "calc-original")).thenReturn(Optional.of(original));
        when(policyRepository.findPublished("1", "product-1", 1, LocalDateTime.of(2026, 8, 20, 12, 0)))
                .thenReturn(Optional.of(publishedPolicy()));
        when(calculationRepository.findByIdempotencyKey(
                "1", "maintenance-1:replacement", PricingCalculationPurpose.MAINTENANCE))
                .thenReturn(Optional.empty());
        when(adjustment.getCustomerAmount()).thenReturn(new BigDecimal("72.72"));
        when(adjustment.getAdjustmentId()).thenReturn("adjustment-1");
        when(lifecycleService.create(any())).thenReturn(adjustment);

        SurrenderValueCalculationResult result = service.calculate(new CalculateSurrenderValueCommand(
                "1", "maintenance-1", "policy-1", "issuance-biz-1", "calc-original", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 8, 20), 1, LocalDateTime.of(2026, 8, 20, 12, 0), "寿险退保",
                null, null, false, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        List<SurrenderFactor> factors = result.factors();
        assertEquals(6, factors.size(), "明细丢失即下游对客展示与对账只剩一个净额，口径无从解释");
        assertEquals(SurrenderFactorType.CASH_VALUE, factors.get(0).factorType());
        assertEquals(ChargeDirection.CREDIT, factors.get(0).direction());
        assertEquals(new BigDecimal("72.72"), factors.get(0).amount());
        assertEquals(SurrenderValueOutcome.EXPLAIN_CASH_VALUE, factors.get(0).explainKey());
        assertEquals(ChargeDirection.NONE, factors.get(1).direction(),
                "现金价值基准下未满期保费因子以 NONE/0 占位，不得从明细中消失");
        assertEquals(0, factors.get(1).amount().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("未满期保费基准：命令下发的保费事实按「年保费 × 剩余天数 / 365」折算，未满期因子为 CREDIT 加项")
    void shouldRefundUnearnedPremiumUnderUnearnedBasis() {
        PremiumCalculation original = originalCalculation();
        PremiumLifecycleAdjustment adjustment = mock(PremiumLifecycleAdjustment.class);
        LocalDate surrenderDate = LocalDate.of(2026, 8, 20);
        when(calculationRepository.findById("1", "calc-original")).thenReturn(Optional.of(original));
        when(policyRepository.findPublished("1", "product-1", 1, LocalDateTime.of(2026, 8, 20, 12, 0)))
                .thenReturn(Optional.of(publishedPolicy(SurrenderRefundBasis.UNEARNED_PREMIUM)));
        when(calculationRepository.findByIdempotencyKey(
                "1", "maintenance-1:replacement", PricingCalculationPurpose.MAINTENANCE))
                .thenReturn(Optional.empty());
        when(adjustment.getCustomerAmount()).thenReturn(new BigDecimal("109.59"));
        when(adjustment.getAdjustmentId()).thenReturn("adjustment-1");
        when(lifecycleService.create(any())).thenReturn(adjustment);

        SurrenderValueCalculationResult result = service.calculate(new CalculateSurrenderValueCommand(
                "1", "maintenance-1", "policy-1", "issuance-biz-1", "calc-original", LocalDate.of(2026, 1, 1),
                surrenderDate, 1, LocalDateTime.of(2026, 8, 20, 12, 0), "寿险退保",
                new BigDecimal("200.00"), surrenderDate.plusDays(200), false,
                BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        // 200.00 × 200 / 365 = 109.589041… → 109.59
        assertEquals(SurrenderRefundBasis.UNEARNED_PREMIUM, result.refundBasis());
        assertEquals(new BigDecimal("109.59"), result.refundAmount());
        assertEquals(0, result.cashValueRate().compareTo(BigDecimal.ZERO),
                "未满期保费基准不适用费率，报 0 而非沿用现金价值率");
        List<SurrenderFactor> factors = result.factors();
        assertEquals(6, factors.size());
        assertEquals(ChargeDirection.NONE, factors.get(0).direction());
        assertEquals(SurrenderFactorType.UNEARNED_PREMIUM, factors.get(1).factorType());
        assertEquals(ChargeDirection.CREDIT, factors.get(1).direction());
        assertEquals(new BigDecimal("109.59"), factors.get(1).amount());
        assertEquals(SurrenderValueOutcome.EXPLAIN_UNEARNED_PREMIUM, factors.get(1).explainKey());
    }

    @Test
    @DisplayName("现金价值基准的请求哈希不受保费事实影响：升级后在途退保请求不会误判幂等冲突")
    void shouldKeepRequestHashStableForCashValueBasis() {
        runCashValueBasis(null, null, ZERO_BALANCE, CASH_VALUE_REFUND);
        runCashValueBasis(new BigDecimal("200.00"), LocalDate.of(2027, 3, 8), ZERO_BALANCE, CASH_VALUE_REFUND);

        ArgumentCaptor<PremiumCalculation> captor = ArgumentCaptor.forClass(PremiumCalculation.class);
        verify(calculationRepository, times(2)).save(captor.capture());
        assertEquals(captor.getAllValues().get(0).getRequestHash(), captor.getAllValues().get(1).getRequestHash(),
                "保费事实只在未满期保费基准下影响结果，一律纳入哈希会让存量在途退保请求升级后失败");
    }

    // ==================== g05-04 欠缴保费与红利/账户价值 ====================

    @Test
    @DisplayName("g05-04：账户余额三项全零不纳入请求哈希（存量在途请求逐字不变），任一非零才纳入")
    void shouldIncludeBalanceFactsInRequestHashOnlyWhenNonZero() {
        runCashValueBasis(null, null, ZERO_BALANCE, CASH_VALUE_REFUND);
        // 无欠费、无红利、无投资账户：与存量「命令根本没有这三项」是同一组事实，哈希须逐字相同
        runCashValueBasis(null, null,
                new SurrenderBalanceFacts(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO), CASH_VALUE_REFUND);
        // 欠费 50.00 先行抵扣 ⇒ 应退 72.72 − 50.00 = 22.72
        runCashValueBasis(null, null,
                new SurrenderBalanceFacts(new BigDecimal("50.00"), BigDecimal.ZERO, BigDecimal.ZERO),
                new BigDecimal("22.72"));

        ArgumentCaptor<PremiumCalculation> captor = ArgumentCaptor.forClass(PremiumCalculation.class);
        verify(calculationRepository, times(3)).save(captor.capture());
        assertEquals(captor.getAllValues().get(0).getRequestHash(), captor.getAllValues().get(1).getRequestHash(),
                "三项全零是「无欠费、无余额」的业务事实，其因子恒为 DEBIT/0 与 CREDIT/0，净额不变，"
                        + "一律纳入哈希会让存量在途退保请求升级后失败");
        assertNotEquals(captor.getAllValues().get(0).getRequestHash(), captor.getAllValues().get(2).getRequestHash(),
                "非零欠费确实改变应退金额，必须进入请求哈希，否则重放会静默复用旧结果");
    }

    @Test
    @DisplayName("g05-04：命令下发的三项余额原样过桥，合并为「红利及账户价值」因子的动作发生在领域层")
    void shouldTransportBalanceFactsWithoutMerging() {
        PremiumCalculation original = originalCalculation();
        PremiumLifecycleAdjustment adjustment = mock(PremiumLifecycleAdjustment.class);
        LocalDate surrenderDate = LocalDate.of(2026, 8, 20);
        when(calculationRepository.findById("1", "calc-original")).thenReturn(Optional.of(original));
        when(policyRepository.findPublished("1", "product-1", 1, LocalDateTime.of(2026, 8, 20, 12, 0)))
                .thenReturn(Optional.of(publishedPolicy(SurrenderRefundBasis.UNEARNED_PREMIUM)));
        when(calculationRepository.findByIdempotencyKey(
                "1", "maintenance-1:replacement", PricingCalculationPurpose.MAINTENANCE))
                .thenReturn(Optional.empty());
        when(adjustment.getCustomerAmount()).thenReturn(new BigDecimal("119.59"));
        when(adjustment.getAdjustmentId()).thenReturn("adjustment-1");
        when(lifecycleService.create(any())).thenReturn(adjustment);

        SurrenderValueCalculationResult result = service.calculate(new CalculateSurrenderValueCommand(
                "1", "maintenance-1", "policy-1", "issuance-biz-1", "calc-original", LocalDate.of(2026, 1, 1),
                surrenderDate, 1, LocalDateTime.of(2026, 8, 20, 12, 0), "寿险退保",
                new BigDecimal("200.00"), surrenderDate.plusDays(200), false,
                BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("20.00"), new BigDecimal("20.00"), new BigDecimal("10.00")));

        // 未满期 200.00 × 200 / 365 = 109.59；欠费 20.00 抵扣；红利 20.00 + 投资账户 10.00 = 30.00 一并退还
        List<SurrenderFactor> factors = result.factors();
        assertEquals(6, factors.size());
        assertEquals(SurrenderFactorType.OVERDUE_PREMIUM, factors.get(3).factorType());
        assertEquals(ChargeDirection.DEBIT, factors.get(3).direction());
        assertEquals(0, factors.get(3).amount().compareTo(new BigDecimal("20.00")));
        assertEquals(SurrenderFactorType.DIVIDEND_ACCOUNT_VALUE, factors.get(4).factorType());
        assertEquals(ChargeDirection.CREDIT, factors.get(4).direction());
        assertEquals(0, factors.get(4).amount().compareTo(new BigDecimal("30.00")),
                "红利与投资账户价值必须合并到同一个因子金额：合并口径属产品域，应用层只搬运原始分量");
        assertEquals(0, result.refundAmount().compareTo(new BigDecimal("119.59")));
    }

    @Test
    @DisplayName("g05-04：账户余额事实缺失即失败关闭，绝不按「无欠费、无余额」以 0 兜底")
    void shouldFailClosedWhenBalanceFactsAbsent() {
        when(calculationRepository.findById("1", "calc-original"))
                .thenReturn(Optional.of(originalCalculation()));
        when(policyRepository.findPublished("1", "product-1", 1, LocalDateTime.of(2026, 8, 20, 12, 0)))
                .thenReturn(Optional.of(publishedPolicy()));

        // 贷款事实已知为零、账户余额事实不可得（三项 null）⇒ 须以账户余额码拒算，而非按「无欠费、无余额」置 0
        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> service.calculate(new CalculateSurrenderValueCommand(
                        "1", "maintenance-1", "policy-1", "issuance-biz-1", "calc-original",
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 8, 20), 1,
                        LocalDateTime.of(2026, 8, 20, 12, 0), "寿险退保",
                        null, null, false, BigDecimal.ZERO, BigDecimal.ZERO,
                        null, null, null)));

        assertEquals(ProductErrorCode.SURRENDER_BALANCE_FACTS_MISSING, ex.getErrorCode());
    }

    @Test
    @DisplayName("g05-04：事实缺失的拒算按因子顺序发生——贷款（索引 2）先于欠费（索引 3），兼容构造器整体置空时先报贷款")
    void shouldReportFirstMissingFactInFactorOrder() {
        when(calculationRepository.findById("1", "calc-original"))
                .thenReturn(Optional.of(originalCalculation()));
        when(policyRepository.findPublished("1", "product-1", 1, LocalDateTime.of(2026, 8, 20, 12, 0)))
                .thenReturn(Optional.of(publishedPolicy()));

        // 十参兼容构造器把保费事实与两项新事实一并置空 ⇒ 首个不可得的是贷款本息
        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> service.calculate(new CalculateSurrenderValueCommand(
                        "1", "maintenance-1", "policy-1", "issuance-biz-1", "calc-original",
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 8, 20), 1,
                        LocalDateTime.of(2026, 8, 20, 12, 0), "寿险退保")));

        assertEquals(ProductErrorCode.SURRENDER_LOAN_FACTS_MISSING, ex.getErrorCode(),
                "错误码须指向真正不可得的那一项：明细按因子顺序构造，先构造的因子先失败");
    }

    @Test
    void shouldRejectOriginalCalculationFromAnotherIssuance() {
        when(calculationRepository.findById("1", "calc-original"))
                .thenReturn(Optional.of(originalCalculation()));

        assertThrows(com.titanium.common.exception.BusinessException.class,
                () -> service.calculate(new CalculateSurrenderValueCommand(
                        "1", "maintenance-1", "policy-1", "other-issuance", "calc-original",
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 8, 20), 1,
                        LocalDateTime.of(2026, 8, 20, 12, 0), "寿险退保")));
    }

    private PremiumCalculation originalCalculation() {
        BigDecimal customer = new BigDecimal("121.20");
        BigDecimal internal = new BigDecimal("9.60");
        return PremiumCalculation.confirm(
                "calc-original", "request-original", "issuance-biz-1", PricingCalculationPurpose.ISSUANCE_CONFIRM,
                "1", "product-1", LocalDateTime.of(2026, 1, 1, 0, 0), "CNY", customer, customer,
                customer, 1, List.of(), new CalculationTotals(customer, BigDecimal.ZERO, customer, internal),
                List.of(
                        line("premium", AmountChannel.CUSTOMER_PRICE, ChargePayerType.POLICYHOLDER, customer, true),
                        line("commission", AmountChannel.INTERNAL_COST, ChargePayerType.INSURER, internal, false)),
                evidence(), Map.of(), hash('q'), hash('i'), hash('r'), LocalDateTime.of(2026, 1, 1, 0, 1));
    }

    private CalculationLine line(
            String code,
            AmountChannel channel,
            ChargePayerType payer,
            BigDecimal amount,
            boolean affectsCustomerPayable) {
        ChargeCategory category = channel == AmountChannel.CUSTOMER_PRICE
                ? ChargeCategory.RISK_PREMIUM
                : ChargeCategory.COMMISSION;
        return new CalculationLine(
                code, code, "V1", category, channel, ChargeDirection.DEBIT, payer, code.toUpperCase(),
                "CNY", amount, BigDecimal.ONE, amount, code, true, code, affectsCustomerPayable, null);
    }

    /** 以现金价值基准策略跑一次退保确认，保费事实与账户余额事实由入参决定（用于验证请求哈希的稳定性）。 */
    private void runCashValueBasis(
            BigDecimal annualPremium,
            LocalDate coverageEndDate,
            SurrenderBalanceFacts balance,
            BigDecimal expectedRefund) {
        PremiumLifecycleAdjustment adjustment = mock(PremiumLifecycleAdjustment.class);
        when(calculationRepository.findById("1", "calc-original")).thenReturn(Optional.of(originalCalculation()));
        when(policyRepository.findPublished("1", "product-1", 1, LocalDateTime.of(2026, 8, 20, 12, 0)))
                .thenReturn(Optional.of(publishedPolicy()));
        when(calculationRepository.findByIdempotencyKey(
                "1", "maintenance-1:replacement", PricingCalculationPurpose.MAINTENANCE))
                .thenReturn(Optional.empty());
        when(adjustment.getCustomerAmount()).thenReturn(expectedRefund);
        when(adjustment.getAdjustmentId()).thenReturn("adjustment-1");
        when(lifecycleService.create(any())).thenReturn(adjustment);

        service.calculate(new CalculateSurrenderValueCommand(
                "1", "maintenance-1", "policy-1", "issuance-biz-1", "calc-original", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 8, 20), 1, LocalDateTime.of(2026, 8, 20, 12, 0), "寿险退保",
                annualPremium, coverageEndDate, false, BigDecimal.ZERO, BigDecimal.ZERO,
                balance.overduePremium(), balance.dividendBalance(), balance.accountValue()));
    }

    private SurrenderValuePolicy publishedPolicy() {
        return publishedPolicy(SurrenderRefundBasis.CASH_VALUE);
    }

    private SurrenderValuePolicy publishedPolicy(SurrenderRefundBasis refundBasis) {
        SurrenderValuePolicy policy = SurrenderValuePolicy.createDraft(
                "surrender-policy-1", "product-1", "LIFE-SURRENDER-CASH-VALUE", "V1.0", 1, 15,
                new BigDecimal("0.60000000"), BigDecimal.ZERO, null, refundBasis,
                LocalDateTime.of(2026, 1, 1, 0, 0), null, "1");
        policy.approve();
        policy.publish();
        return policy;
    }

    private PremiumCalculationEvidence evidence() {
        return new PremiumCalculationEvidence(
                "V1.0", "P1", hash('p'), "TABLE", "V1", hash('t'), "feature-1",
                "RULE", "V1", hash('a'), 2, "HALF_UP");
    }

    private String hash(char value) {
        return String.valueOf(value).repeat(64);
    }
}
