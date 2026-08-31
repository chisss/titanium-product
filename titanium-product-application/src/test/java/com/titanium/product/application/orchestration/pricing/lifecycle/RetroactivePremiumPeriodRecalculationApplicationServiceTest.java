package com.titanium.product.application.orchestration.pricing.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.titanium.common.exception.BusinessException;
import com.titanium.product.aggregate.PremiumCalculation;
import com.titanium.product.application.orchestration.pricing.PricingEvidenceHasher;
import com.titanium.product.command.pricing.lifecycle.RecalculateRetroactivePremiumPeriodsCommand;
import com.titanium.product.command.pricing.lifecycle.RecalculateRetroactivePremiumPeriodsCommand.AffectedPeriod;
import com.titanium.product.common.enums.PremiumBalanceDirection;
import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.repository.PremiumCalculationRepository;
import com.titanium.product.valueobject.pricing.PremiumCalculationEvidence;

class RetroactivePremiumPeriodRecalculationApplicationServiceTest {

    private static final LocalDateTime SCOPE_FROM = LocalDateTime.of(2026, 6, 1, 0, 0);
    private static final LocalDateTime SCOPE_TO = LocalDateTime.of(2026, 8, 26, 0, 0);

    private PremiumCalculationRepository repository;
    private RetroactivePremiumPeriodRecalculationApplicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(PremiumCalculationRepository.class);
        service = new RetroactivePremiumPeriodRecalculationApplicationService(
                repository, new PricingEvidenceHasher());
        when(repository.findById("tenant-1", "calc-original"))
                .thenReturn(Optional.of(calculation("calc-original", "100.00", "CNY")));
        when(repository.findById("tenant-1", "calc-replacement"))
                .thenReturn(Optional.of(calculation("calc-replacement", "120.00", "CNY")));
    }

    @Test
    void shouldCalculateVersionedDifferencesForEveryAffectedPeriod() {
        var result = service.recalculate(command(List.of(
                period("period-1", LocalDateTime.of(2026, 7, 1, 0, 0), "100.00", "CNY", 'c'),
                period("period-2", LocalDateTime.of(2026, 8, 1, 0, 0), "130.00", "CNY", 'd'))));

        assertEquals("PERIOD_V1", result.recalculationVersion());
        assertEquals(PremiumBalanceDirection.DEBIT, result.direction());
        assertEquals(new BigDecimal("10.00"), result.amount());
        assertEquals(PremiumBalanceDirection.DEBIT, result.periods().getFirst().direction());
        assertEquals(PremiumBalanceDirection.CREDIT, result.periods().get(1).direction());
        assertEquals(64, result.resultHash().length());
    }

    @Test
    void shouldReturnNoDifferenceForEmptyAffectedPeriods() {
        var result = service.recalculate(command(List.of()));

        assertEquals(PremiumBalanceDirection.NONE, result.direction());
        assertEquals(BigDecimal.ZERO, result.amount());
        assertEquals(List.of(), result.periods());
    }

    @Test
    void shouldRejectPeriodOutsideScopeOrWithDifferentCurrency() {
        assertThrows(BusinessException.class, () -> service.recalculate(command(List.of(
                period("period-1", SCOPE_FROM, "100.00", "CNY", 'c')))));
        assertThrows(BusinessException.class, () -> service.recalculate(command(List.of(
                period("period-1", LocalDateTime.of(2026, 7, 1, 0, 0), "100.00", "USD", 'c')))));
    }

    private RecalculateRetroactivePremiumPeriodsCommand command(List<AffectedPeriod> periods) {
        return new RecalculateRetroactivePremiumPeriodsCommand(
                "tenant-1", "request-1", "case-1", "policy-1", "analysis-1", 1,
                hash('a'), "calc-original", "calc-replacement", SCOPE_FROM, SCOPE_TO, periods);
    }

    private AffectedPeriod period(
            String id, LocalDateTime start, String amount, String currency, char evidenceHash) {
        return new AffectedPeriod(
                id, "bill-" + id, start, new BigDecimal(amount), currency, hash(evidenceHash));
    }

    private PremiumCalculation calculation(String id, String amount, String currency) {
        BigDecimal premium = new BigDecimal(amount);
        return PremiumCalculation.confirm(
                id, "request-" + id, "policy-1", PricingCalculationPurpose.MAINTENANCE,
                "tenant-1", "product-1", LocalDateTime.of(2026, 6, 1, 0, 0), currency,
                premium, premium, premium, 1, List.of(), evidence(), Map.of(),
                hash('q'), hash('i'), hash(id.equals("calc-original") ? 'o' : 'r'),
                LocalDateTime.of(2026, 6, 1, 0, 1));
    }

    private PremiumCalculationEvidence evidence() {
        return new PremiumCalculationEvidence(
                "V1.0", "P1", hash('p'), "TABLE", "V1", hash('t'), "feature-1",
                "RULE", "V1", hash('e'), 2, "HALF_UP");
    }

    private String hash(char value) {
        return String.valueOf((char) ('a' + Math.floorMod(value, 6))).repeat(64);
    }
}
