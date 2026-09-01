package com.titanium.product.application.orchestration.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.application.orchestration.pricing.validation.PremiumCalculationCommandValidator;
import com.titanium.product.application.service.pricing.PremiumQuoteApplicationService;
import com.titanium.product.command.pricing.PremiumCalculationCommand;
import com.titanium.product.common.enums.PremiumAdjustmentType;
import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.pricing.aggregate.PremiumCalculation;
import com.titanium.product.repository.PremiumCalculationRepository;
import com.titanium.product.service.CalculationTotalsService;
import com.titanium.product.service.PremiumAdjustmentService;
import com.titanium.product.service.PremiumCalculationBreakdownService;
import com.titanium.product.valueobject.pricing.premium.PremiumAdjustmentRequest;
import com.titanium.product.valueobject.pricing.premium.PremiumQuote;

class PremiumCalculationApplicationServiceTest {

    private static final LocalDateTime BUSINESS_TIME = LocalDateTime.of(2026, 8, 18, 12, 0);

    private PremiumQuoteApplicationService quoteService;
    private PremiumCalculationRepository repository;
    private PremiumCalculationApplicationService service;

    @BeforeEach
    void setUp() {
        quoteService = mock(PremiumQuoteApplicationService.class);
        repository = mock(PremiumCalculationRepository.class);
        service = new PremiumCalculationApplicationService(
                quoteService, repository, new PremiumAdjustmentService(),
                new PremiumCalculationBreakdownService(new CalculationTotalsService()),
                new PricingEvidenceHasher(), new PremiumCalculationCommandValidator());
        when(quoteService.quote(any())).thenReturn(quote());
    }

    @Test
    void shouldPersistConfirmedPremiumWithUnderwritingAdjustment() {
        when(repository.findByIdempotencyKey("tenant-a", "calc-1", PricingCalculationPurpose.ISSUANCE_CONFIRM))
                .thenReturn(Optional.empty());

        PremiumCalculation calculation = service.confirm(command("calc-1", "proposal-1", "100000"));

        assertEquals(new BigDecimal("1000.00"), calculation.getStandardPremium());
        assertEquals(new BigDecimal("1100.00"), calculation.getTotalPremium());
        assertEquals(new BigDecimal("91.67"), calculation.getInstallmentAmount());
        assertEquals("P1", calculation.getEvidence().pricingPlanVersion());
        assertEquals("UW-SURCHARGE", calculation.getAdjustments().getFirst().adjustmentCode());
        assertEquals(new BigDecimal("1100.00"), calculation.getCalculationTotals().customerPayable());
        assertEquals(2, calculation.getCalculationLines().size());
        verify(repository).save(any(PremiumCalculation.class));
    }

    @Test
    void shouldPersistMaintenancePremiumCalculation() {
        when(repository.findByIdempotencyKey("tenant-a", "maintenance-1", PricingCalculationPurpose.MAINTENANCE))
                .thenReturn(Optional.empty());

        PremiumCalculation calculation = service.confirm(command(
                "maintenance-1", "policy-1", "100000", PricingCalculationPurpose.MAINTENANCE));

        assertEquals(PricingCalculationPurpose.MAINTENANCE, calculation.getPurpose());
        verify(repository).save(any(PremiumCalculation.class));
    }

    @Test
    void shouldRejectUnexpectedPricingPlanBeforePersistence() {
        when(repository.findByIdempotencyKey("tenant-a", "maintenance-plan", PricingCalculationPurpose.MAINTENANCE))
                .thenReturn(Optional.empty());
        PremiumCalculationCommand command = new PremiumCalculationCommand(
                "tenant-a", "product-1", "maintenance-plan", "policy-1",
                PricingCalculationPurpose.MAINTENANCE, "V1.0", "P2", BUSINESS_TIME,
                "CNY", new BigDecimal("100000"), 35, "M", 10, 20, 12,
                Map.of("insured.age", 35), List.of(), null, 1);

        assertThrows(BusinessException.class, () -> service.confirm(command));

        verify(repository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void shouldRejectMissingCalculationPurpose() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.confirm(command("calc-1", "proposal-1", "100000", null)));

        assertEquals(ProductErrorCode.PRICING_INPUT_INVALID.getCode(), exception.getErrorCode());
    }

    @Test
    void shouldReturnExistingCalculationWithoutRecomputing() {
        when(repository.findByIdempotencyKey("tenant-a", "calc-1", PricingCalculationPurpose.ISSUANCE_CONFIRM))
                .thenReturn(Optional.empty());
        PremiumCalculation first = service.confirm(command("calc-1", "proposal-1", "100000"));
        when(repository.findByIdempotencyKey("tenant-a", "calc-1", PricingCalculationPurpose.ISSUANCE_CONFIRM))
                .thenReturn(Optional.of(first));

        PremiumCalculation retry = service.confirm(command("calc-1", "proposal-1", "100000"));

        assertEquals(first.getCalculationId(), retry.getCalculationId());
        verify(quoteService).quote(any());
    }

    @Test
    void shouldRejectSameCalculationRequestWithDifferentInput() {
        when(repository.findByIdempotencyKey("tenant-a", "calc-1", PricingCalculationPurpose.ISSUANCE_CONFIRM))
                .thenReturn(Optional.empty());
        PremiumCalculation first = service.confirm(command("calc-1", "proposal-1", "100000"));
        when(repository.findByIdempotencyKey("tenant-a", "calc-1", PricingCalculationPurpose.ISSUANCE_CONFIRM))
                .thenReturn(Optional.of(first));

        PricingDomainException exception = assertThrows(
                PricingDomainException.class, () -> service.confirm(command("calc-1", "proposal-1", "110000")));

        assertEquals(ProductErrorCode.PRICING_IDEMPOTENCY_CONFLICT.getCode(), exception.getErrorCode());
        verify(quoteService).quote(any());
    }

    private PremiumCalculationCommand command(String requestId, String bizNo, String sumInsured) {
        return command(requestId, bizNo, sumInsured, PricingCalculationPurpose.ISSUANCE_CONFIRM);
    }

    private PremiumCalculationCommand command(
            String requestId, String bizNo, String sumInsured, PricingCalculationPurpose purpose) {
        return new PremiumCalculationCommand(
                "tenant-a", "product-1", requestId, bizNo, purpose,
                "V1.0", BUSINESS_TIME, "CNY", new BigDecimal(sumInsured), 35, "M", 10, 20, 12,
                Map.of("insured.age", 35), List.of(new PremiumAdjustmentRequest(
                        "UW-SURCHARGE", PremiumAdjustmentType.SURCHARGE_RATE,
                        new BigDecimal("0.10"), "核保加费", "UW-V1")));
    }

    private PremiumQuote quote() {
        return new PremiumQuote(
                "quote-1", "calc-1", "product-1", "V1.0", "CNY", new BigDecimal("1000.00"),
                new BigDecimal("83.33"), 12, new BigDecimal("0.01"), "row-1", "LIFE_BASE", "V1",
                "table-hash", "P1", "plan-hash", "feature-1", "formula", "V1", "artifact-hash",
                2, RoundingMode.HALF_UP.name(), "input-hash", "result-hash");
    }
}
