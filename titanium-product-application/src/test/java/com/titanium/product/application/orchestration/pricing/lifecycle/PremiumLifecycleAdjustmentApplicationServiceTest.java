package com.titanium.product.application.orchestration.pricing.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.product.aggregate.PremiumCalculation;
import com.titanium.product.aggregate.lifecycle.PremiumLifecycleAdjustment;
import com.titanium.product.application.orchestration.pricing.PricingEvidenceHasher;
import com.titanium.product.command.pricing.lifecycle.CreatePremiumLifecycleAdjustmentCommand;
import com.titanium.product.command.pricing.lifecycle.CreatePremiumLifecycleReversalCommand;
import com.titanium.product.common.enums.PremiumBalanceDirection;
import com.titanium.product.common.enums.PremiumLifecycleType;
import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.repository.PremiumCalculationRepository;
import com.titanium.product.repository.PremiumLifecycleAdjustmentRepository;
import com.titanium.product.valueobject.pricing.CalculationLine;
import com.titanium.product.valueobject.pricing.CalculationTotals;
import com.titanium.product.valueobject.pricing.PremiumCalculationEvidence;
import com.titanium.product.valueobject.pricing.lifecycle.PremiumLifecycleDifference;

class PremiumLifecycleAdjustmentApplicationServiceTest {

    private PremiumCalculationRepository calculationRepository;
    private PremiumLifecycleAdjustmentRepository adjustmentRepository;
    private PremiumLifecycleAdjustmentApplicationService service;

    @BeforeEach
    void setUp() {
        calculationRepository = mock(PremiumCalculationRepository.class);
        adjustmentRepository = mock(PremiumLifecycleAdjustmentRepository.class);
        service = new PremiumLifecycleAdjustmentApplicationService(
                calculationRepository, adjustmentRepository, new PricingEvidenceHasher());
        when(calculationRepository.findById("tenant-a", "original"))
                .thenReturn(Optional.of(calculation("original", PricingCalculationPurpose.ISSUANCE_CONFIRM, "100")));
        when(calculationRepository.findById("tenant-a", "replacement"))
                .thenReturn(Optional.of(calculation("replacement", PricingCalculationPurpose.MAINTENANCE, "120")));
    }

    @Test
    void shouldPersistImmutableLifecycleAdjustment() {
        when(adjustmentRepository.findByRequestId("tenant-a", "adjustment-1"))
                .thenReturn(Optional.empty());

        PremiumLifecycleAdjustment adjustment = service.create(command("adjustment-1", "批改增额"));

        assertEquals(PremiumBalanceDirection.DEBIT, adjustment.getDirection());
        assertEquals(new BigDecimal("20"), adjustment.getCustomerAmount());
        assertEquals(64, adjustment.getResultHash().length());
        assertEquals(0, adjustment.getCreatedAt().getNano());
        verify(adjustmentRepository).save(any(PremiumLifecycleAdjustment.class));
    }

    @Test
    void shouldRejectDifferentRequestUsingSameIdempotencyKey() {
        when(adjustmentRepository.findByRequestId("tenant-a", "adjustment-1"))
                .thenReturn(Optional.empty());
        PremiumLifecycleAdjustment existing = service.create(command("adjustment-1", "批改增额"));
        when(adjustmentRepository.findByRequestId("tenant-a", "adjustment-1"))
                .thenReturn(Optional.of(existing));

        assertThrows(PricingDomainException.class,
                () -> service.create(command("adjustment-1", "不同原因")));
    }

    @Test
    void shouldCreateReverseAdjustmentWithOppositeCustomerDirection() {
        when(adjustmentRepository.findByRequestId("tenant-a", "adjustment-1"))
                .thenReturn(Optional.empty());
        PremiumLifecycleAdjustment source = service.create(command("adjustment-1", "退保贷项"));
        when(adjustmentRepository.findById("tenant-a", "adjustment-1"))
                .thenReturn(Optional.of(source));
        when(adjustmentRepository.findByRequestId("tenant-a", "reversal-1"))
                .thenReturn(Optional.empty());

        PremiumLifecycleAdjustment reversal = service.createReversal(new CreatePremiumLifecycleReversalCommand(
                "tenant-a", "reversal-1", "adjustment-1", LocalDateTime.of(2026, 8, 21, 10, 0), "退保撤销"));

        assertEquals(PremiumLifecycleType.REVERSAL, reversal.getLifecycleType());
        assertEquals(PremiumBalanceDirection.DEBIT, source.getDirection());
        assertEquals(PremiumBalanceDirection.CREDIT, reversal.getDirection());
        assertEquals(source.getAdjustmentId(), reversal.getReversalOfAdjustmentId());
        assertEquals(source.getReplacementCalculationId(), reversal.getOriginalCalculationId());
        verify(adjustmentRepository, org.mockito.Mockito.times(2)).save(any(PremiumLifecycleAdjustment.class));
    }

    @Test
    void shouldRejectSecondReversalForSameSourceAdjustment() {
        when(adjustmentRepository.findByRequestId("tenant-a", "adjustment-1"))
                .thenReturn(Optional.empty());
        PremiumLifecycleAdjustment source = service.create(command("adjustment-1", "退保贷项"));
        when(adjustmentRepository.findById("tenant-a", "adjustment-1"))
                .thenReturn(Optional.of(source));
        when(adjustmentRepository.findByRequestId("tenant-a", "reversal-2"))
                .thenReturn(Optional.empty());
        when(adjustmentRepository.findByReversalOfAdjustmentId("tenant-a", "adjustment-1"))
                .thenReturn(Optional.of(PremiumLifecycleAdjustment.confirmReversal(
                        "reversal-1", "reversal-1", "adjustment-1", source.getBizNo(),
                        PremiumLifecycleType.REVERSAL, "tenant-a", source.getProductId(),
                        source.getReplacementCalculationId(), source.getReplacementResultHash(),
                        source.getOriginalCalculationId(), source.getOriginalResultHash(),
                        LocalDateTime.of(2026, 8, 21, 10, 0), source.getCurrency(),
                        new PremiumLifecycleDifference(
                                PremiumBalanceDirection.CREDIT, source.getCustomerAmount(),
                                source.getTaxDirection(), source.getTaxAmount(),
                                source.getInternalCostDirection(), source.getInternalCostAmount(), source.getLines()),
                        "已冲正", hash('q'), hash('r'), LocalDateTime.of(2026, 8, 21, 10, 1))));

        assertThrows(PricingDomainException.class, () -> service.createReversal(
                new CreatePremiumLifecycleReversalCommand(
                        "tenant-a", "reversal-2", "adjustment-1",
                        LocalDateTime.of(2026, 8, 21, 11, 0), "重复冲正")));
    }

    private CreatePremiumLifecycleAdjustmentCommand command(String requestId, String reason) {
        return new CreatePremiumLifecycleAdjustmentCommand(
                "tenant-a", requestId, "maintenance-1", PremiumLifecycleType.ENDORSEMENT,
                "original", "replacement", LocalDateTime.of(2026, 8, 20, 12, 0), reason);
    }

    private PremiumCalculation calculation(
            String id, PricingCalculationPurpose purpose, String premium) {
        BigDecimal amount = new BigDecimal(premium);
        return PremiumCalculation.confirm(
                id, "request-" + id, "policy-1", purpose, "tenant-a", "product-1",
                LocalDateTime.of(2026, 8, 20, 12, 0), "CNY", amount, amount, amount, 1,
                List.of(), new CalculationTotals(amount, BigDecimal.ZERO, amount, BigDecimal.ZERO),
                List.of(new CalculationLine(
                        "premium", "premium", "V1", ChargeCategory.RISK_PREMIUM,
                        AmountChannel.CUSTOMER_PRICE, ChargeDirection.DEBIT, ChargePayerType.POLICYHOLDER,
                        "PREMIUM", "CNY", amount, BigDecimal.ONE, amount, "premium", true,
                        "标准保费", true, null)), evidence(), Map.of(), hash('q'), hash('i'), hash(id.charAt(0)),
                LocalDateTime.of(2026, 8, 20, 12, 1));
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
