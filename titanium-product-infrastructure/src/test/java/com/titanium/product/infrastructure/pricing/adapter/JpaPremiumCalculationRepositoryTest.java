package com.titanium.product.infrastructure.pricing.adapter;

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
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import com.titanium.product.aggregate.PremiumCalculation;
import com.titanium.product.common.enums.PremiumAdjustmentType;
import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.exception.PremiumCalculationConcurrentConflictException;
import com.titanium.product.infrastructure.pricing.entity.PremiumCalculationEntity;
import com.titanium.product.infrastructure.pricing.repository.CalculationLineJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.CalculationTotalJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.PremiumCalculationJpaRepository;
import com.titanium.product.valueobject.pricing.PremiumAdjustment;
import com.titanium.product.valueobject.pricing.PremiumCalculationEvidence;

class JpaPremiumCalculationRepositoryTest {

    private PremiumCalculationJpaRepository jpaRepository;
    private CalculationTotalJpaRepository calculationTotalJpaRepository;
    private CalculationLineJpaRepository calculationLineJpaRepository;
    private JpaPremiumCalculationRepository repository;

    @BeforeEach
    void setUp() {
        jpaRepository = mock(PremiumCalculationJpaRepository.class);
        calculationTotalJpaRepository = mock(CalculationTotalJpaRepository.class);
        calculationLineJpaRepository = mock(CalculationLineJpaRepository.class);
        repository = new JpaPremiumCalculationRepository(
                jpaRepository, calculationTotalJpaRepository, calculationLineJpaRepository);
    }

    @Test
    void shouldPersistAndRestoreImmutableSnapshot() {
        PremiumCalculation source = calculation();

        repository.save(source);

        ArgumentCaptor<PremiumCalculationEntity> captor = ArgumentCaptor.forClass(PremiumCalculationEntity.class);
        verify(jpaRepository).saveAndFlush(captor.capture());
        verify(calculationTotalJpaRepository).save(any());
        verify(calculationLineJpaRepository).saveAll(any());
        PremiumCalculationEntity entity = captor.getValue();
        assertEquals("V1.0", entity.getProductVersion());
        assertEquals("P1", entity.getPricingPlanVersion());
        assertEquals(source.getCreatedAt(), entity.getCreateTime());
        entity.setCreateTime(LocalDateTime.of(2026, 8, 18, 12, 1));
        when(jpaRepository.findByCalculationIdAndTenantId("calculation-1", "tenant-a"))
                .thenReturn(Optional.of(entity));

        PremiumCalculation restored = repository.findById("tenant-a", "calculation-1").orElseThrow();

        assertEquals(new BigDecimal("1100.00"), restored.getTotalPremium());
        assertEquals("UW-SURCHARGE", restored.getAdjustments().getFirst().adjustmentCode());
        assertEquals(35, restored.getRequestSnapshot().get("insured.age"));
        assertEquals("artifact-hash", restored.getEvidence().ruleArtifactHash());
    }

    @Test
    void shouldTranslateUniqueKeyViolationToConcurrentConflict() {
        when(jpaRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(PremiumCalculationConcurrentConflictException.class, () -> repository.save(calculation()));
    }

    private PremiumCalculation calculation() {
        return PremiumCalculation.confirm(
                "calculation-1", "calc-1", "proposal-1", PricingCalculationPurpose.ISSUANCE_CONFIRM,
                "tenant-a", "product-1", LocalDateTime.of(2026, 8, 18, 12, 0), "CNY",
                new BigDecimal("1000.00"), new BigDecimal("1100.00"), new BigDecimal("91.67"), 12,
                List.of(new PremiumAdjustment(
                        "UW-SURCHARGE", PremiumAdjustmentType.SURCHARGE_RATE, new BigDecimal("0.10"),
                        new BigDecimal("100.00"), new BigDecimal("1100.00"), "核保加费", "UW-V1")),
                new PremiumCalculationEvidence(
                        "V1.0", "P1", "plan-hash", "LIFE_BASE", "V1", "table-hash",
                        "feature-1", "formula", "V1", "artifact-hash", 2, "HALF_UP"),
                Map.of("insured.age", 35), hash('a'), hash('i'), hash('o'),
                LocalDateTime.of(2026, 8, 18, 12, 1));
    }

    private String hash(char value) {
        return String.valueOf(value).repeat(64);
    }
}
