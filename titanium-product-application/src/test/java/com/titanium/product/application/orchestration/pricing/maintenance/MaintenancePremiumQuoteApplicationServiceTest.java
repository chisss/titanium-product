package com.titanium.product.application.orchestration.pricing.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.titanium.common.exception.BusinessException;
import com.titanium.product.application.model.pricing.MaintenancePremiumQuoteResult;
import com.titanium.product.application.orchestration.pricing.PremiumCalculationApplicationService;
import com.titanium.product.application.orchestration.pricing.PricingEvidenceHasher;
import com.titanium.product.application.orchestration.pricing.lifecycle.PremiumLifecycleAdjustmentApplicationService;
import com.titanium.product.command.maintenance.CreateMaintenancePremiumQuoteCommand;
import com.titanium.product.command.maintenance.CreateMaintenancePremiumQuoteCommand.SnapshotReference;
import com.titanium.product.command.pricing.PremiumCalculationCommand;
import com.titanium.product.common.enums.PremiumBalanceDirection;
import com.titanium.product.common.enums.PremiumLifecycleType;
import com.titanium.product.pricing.aggregate.PremiumCalculation;
import com.titanium.product.pricing.aggregate.lifecycle.PremiumLifecycleAdjustment;
import com.titanium.product.repository.PremiumCalculationRepository;
import com.titanium.product.valueobject.pricing.premium.PremiumCalculationEvidence;

class MaintenancePremiumQuoteApplicationServiceTest {

    private PremiumCalculationApplicationService calculationService;
    private PremiumLifecycleAdjustmentApplicationService lifecycleService;
    private PremiumCalculationRepository calculationRepository;
    private MaintenancePremiumQuoteApplicationService service;

    @BeforeEach
    void setUp() {
        calculationService = mock(PremiumCalculationApplicationService.class);
        lifecycleService = mock(PremiumLifecycleAdjustmentApplicationService.class);
        calculationRepository = mock(PremiumCalculationRepository.class);
        service = new MaintenancePremiumQuoteApplicationService(
                calculationService, lifecycleService, calculationRepository, new PricingEvidenceHasher());
        PremiumCalculation original = mock(PremiumCalculation.class);
        when(original.getProductId()).thenReturn("product-1");
        when(original.getCurrency()).thenReturn("CNY");
        when(original.getBizNo()).thenReturn("policy-lineage-1");
        when(calculationRepository.findById("tenant-1", "original-calc"))
                .thenReturn(Optional.of(original));
    }

    @Test
    void shouldCreateVersionedQuoteUsingFrozenPricingPlan() {
        PremiumCalculation replacement = replacement();
        PremiumLifecycleAdjustment adjustment = adjustment();
        when(calculationService.confirm(any())).thenReturn(replacement);
        when(lifecycleService.create(any())).thenReturn(adjustment);
        CreateMaintenancePremiumQuoteCommand command = hashedCommand("operation-1");

        MaintenancePremiumQuoteResult result = service.quote(command);

        assertEquals("adjustment-1", result.quoteId());
        assertEquals("r".repeat(64), result.quoteVersion());
        assertEquals(LocalDateTime.parse("2026-08-25T10:00:00"), result.quotedAt());
        assertEquals(LocalDateTime.parse("2026-08-26T10:00:00"), result.validUntil());
        ArgumentCaptor<PremiumCalculationCommand> captor =
                ArgumentCaptor.forClass(PremiumCalculationCommand.class);
        verify(calculationService).confirm(captor.capture());
        assertEquals("plan-v2", captor.getValue().expectedPricingPlanVersion());
        assertEquals("policy-lineage-1", captor.getValue().bizNo());
        assertEquals("case-1", captor.getValue().requestSnapshot().get("maintenanceId"));
        assertEquals(command.idempotencyKey(), captor.getValue().calculationRequestId());
        verify(lifecycleService).create(any());
    }

    @Test
    void shouldRejectForgedPayloadHashBeforeCalculation() {
        CreateMaintenancePremiumQuoteCommand command = command("operation-1", "0".repeat(64));

        assertThrows(BusinessException.class, () -> service.quote(command));

        verifyNoInteractions(calculationService, lifecycleService);
    }

    @Test
    void shouldBindPayloadAndIdempotencyToOperation() {
        CreateMaintenancePremiumQuoteCommand first = command("operation-1", "0".repeat(64));
        CreateMaintenancePremiumQuoteCommand second = command("operation-2", "0".repeat(64));

        assertNotEquals(service.payloadHash(first), service.payloadHash(second));
        assertNotEquals(first.idempotencyKey(), second.idempotencyKey());
    }

    private CreateMaintenancePremiumQuoteCommand hashedCommand(String operationId) {
        CreateMaintenancePremiumQuoteCommand unhashed = command(operationId, "0".repeat(64));
        return command(operationId, service.payloadHash(unhashed));
    }

    private CreateMaintenancePremiumQuoteCommand command(String operationId, String payloadHash) {
        OffsetDateTime capturedAt = OffsetDateTime.of(
                2026, 8, 25, 8, 0, 0, 0, ZoneOffset.ofHours(8));
        return new CreateMaintenancePremiumQuoteCommand(
                "tenant-1", "product-1", "case-1", "policy-1", 7L,
                "COVERAGE_AMOUNT_CHANGE", "product-v3", "plan-v2",
                PremiumLifecycleType.ENDORSEMENT,
                new SnapshotReference("before.json", "a".repeat(64), 7L, capturedAt),
                new SnapshotReference("proposed.json", "b".repeat(64), 7L, capturedAt.plusMinutes(5)),
                "original-calc", LocalDateTime.parse("2026-08-25T09:00:00"), "CNY",
                new BigDecimal("500000"), 35, "M", 10, 20, 12,
                Map.of("insured.occupation", "1"), List.of(), "agent", 3,
                "保额增加", "idempotency-" + operationId, payloadHash);
    }

    private PremiumCalculation replacement() {
        PremiumCalculation replacement = mock(PremiumCalculation.class);
        PremiumCalculationEvidence evidence = mock(PremiumCalculationEvidence.class);
        when(evidence.pricingPlanVersion()).thenReturn("plan-v2");
        when(evidence.pricingPlanContentHash()).thenReturn("p".repeat(64));
        when(replacement.getCalculationId()).thenReturn("replacement-calc");
        when(replacement.getEvidence()).thenReturn(evidence);
        return replacement;
    }

    private PremiumLifecycleAdjustment adjustment() {
        PremiumLifecycleAdjustment adjustment = mock(PremiumLifecycleAdjustment.class);
        when(adjustment.getAdjustmentId()).thenReturn("adjustment-1");
        when(adjustment.getOriginalCalculationId()).thenReturn("original-calc");
        when(adjustment.getOriginalResultHash()).thenReturn("o".repeat(64));
        when(adjustment.getReplacementCalculationId()).thenReturn("replacement-calc");
        when(adjustment.getReplacementResultHash()).thenReturn("n".repeat(64));
        when(adjustment.getResultHash()).thenReturn("r".repeat(64));
        when(adjustment.getDirection()).thenReturn(PremiumBalanceDirection.DEBIT);
        when(adjustment.getCustomerAmount()).thenReturn(new BigDecimal("20.00"));
        when(adjustment.getCurrency()).thenReturn("CNY");
        when(adjustment.getLines()).thenReturn(List.of());
        when(adjustment.getCreatedAt()).thenReturn(LocalDateTime.parse("2026-08-25T10:00:00"));
        return adjustment;
    }
}
