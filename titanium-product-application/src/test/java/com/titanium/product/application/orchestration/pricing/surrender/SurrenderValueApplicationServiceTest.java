package com.titanium.product.application.orchestration.pricing.surrender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.product.aggregate.PremiumCalculation;
import com.titanium.product.aggregate.lifecycle.PremiumLifecycleAdjustment;
import com.titanium.product.aggregate.surrender.SurrenderValuePolicy;
import com.titanium.product.application.model.pricing.surrender.SurrenderValueCalculationResult;
import com.titanium.product.application.orchestration.pricing.PricingEvidenceHasher;
import com.titanium.product.application.orchestration.pricing.lifecycle.PremiumLifecycleAdjustmentApplicationService;
import com.titanium.product.command.pricing.lifecycle.CreatePremiumLifecycleAdjustmentCommand;
import com.titanium.product.command.pricing.surrender.CalculateSurrenderValueCommand;
import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.repository.PremiumCalculationRepository;
import com.titanium.product.repository.SurrenderValuePolicyRepository;
import com.titanium.product.valueobject.pricing.CalculationLine;
import com.titanium.product.valueobject.pricing.CalculationTotals;
import com.titanium.product.valueobject.pricing.PremiumCalculationEvidence;

class SurrenderValueApplicationServiceTest {

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
                LocalDate.of(2026, 8, 20), 1, LocalDateTime.of(2026, 8, 20, 12, 0), "寿险退保"));

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

    private SurrenderValuePolicy publishedPolicy() {
        SurrenderValuePolicy policy = SurrenderValuePolicy.createDraft(
                "surrender-policy-1", "product-1", "LIFE-SURRENDER-CASH-VALUE", "V1.0", 1, 15,
                new BigDecimal("0.60000000"), BigDecimal.ZERO,
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
