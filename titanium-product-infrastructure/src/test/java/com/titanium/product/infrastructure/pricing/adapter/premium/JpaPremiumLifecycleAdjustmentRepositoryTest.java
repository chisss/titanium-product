package com.titanium.product.infrastructure.pricing.adapter.premium;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.product.common.enums.PremiumBalanceDirection;
import com.titanium.product.common.enums.PremiumLifecycleType;
import com.titanium.product.infrastructure.mapper.PremiumLifecycleAdjustmentPersistenceMapperImpl;
import com.titanium.product.infrastructure.pricing.entity.premium.PremiumLifecycleAdjustmentDO;
import com.titanium.product.infrastructure.pricing.entity.premium.PremiumLifecycleDifferenceLineDO;
import com.titanium.product.infrastructure.pricing.repository.premium.PremiumLifecycleAdjustmentJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.premium.PremiumLifecycleDifferenceLineJpaRepository;
import com.titanium.product.pricing.aggregate.lifecycle.PremiumLifecycleAdjustment;
import com.titanium.product.valueobject.pricing.premium.PremiumLifecycleDifference;
import com.titanium.product.valueobject.pricing.premium.PremiumLifecycleDifferenceLine;

class JpaPremiumLifecycleAdjustmentRepositoryTest {

    private PremiumLifecycleAdjustmentJpaRepository adjustmentJpaRepository;
    private PremiumLifecycleDifferenceLineJpaRepository lineJpaRepository;
    private JpaPremiumLifecycleAdjustmentRepository repository;

    @BeforeEach
    void setUp() {
        adjustmentJpaRepository = mock(PremiumLifecycleAdjustmentJpaRepository.class);
        lineJpaRepository = mock(PremiumLifecycleDifferenceLineJpaRepository.class);
        repository = new JpaPremiumLifecycleAdjustmentRepository(
                adjustmentJpaRepository, lineJpaRepository, new PremiumLifecycleAdjustmentPersistenceMapperImpl());
    }

    @Test
    void shouldPersistAndRestoreLifecycleAdjustmentHeaderAndLines() {
        PremiumLifecycleAdjustment source = adjustment();
        AtomicReference<List<PremiumLifecycleDifferenceLineDO>> savedLines = new AtomicReference<>();
        when(lineJpaRepository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<PremiumLifecycleDifferenceLineDO> entities = invocation.getArgument(0);
            List<PremiumLifecycleDifferenceLineDO> values =
                    StreamSupport.stream(entities.spliterator(), false).toList();
            savedLines.set(values);
            return values;
        });

        repository.save(source);

        ArgumentCaptor<PremiumLifecycleAdjustmentDO> headerCaptor =
                ArgumentCaptor.forClass(PremiumLifecycleAdjustmentDO.class);
        verify(adjustmentJpaRepository).saveAndFlush(headerCaptor.capture());
        verify(lineJpaRepository).saveAll(any());
        PremiumLifecycleAdjustmentDO savedHeader = headerCaptor.getValue();
        assertSavedHeader(source, savedHeader);
        assertSavedLines(source, savedLines.get());

        savedHeader.setCreatedAt(source.getCreatedAt());
        when(adjustmentJpaRepository.findByAdjustmentIdAndTenantId("adjustment-1", "tenant-a"))
                .thenReturn(Optional.of(savedHeader));
        when(adjustmentJpaRepository.findByTenantIdAndAdjustmentRequestId("tenant-a", "request-1"))
                .thenReturn(Optional.of(savedHeader));
        when(lineJpaRepository.findByIdAdjustmentIdOrderByIdLineIdAsc("adjustment-1"))
                .thenReturn(savedLines.get());

        PremiumLifecycleAdjustment restoredById =
                repository.findById("tenant-a", "adjustment-1").orElseThrow();
        PremiumLifecycleAdjustment restoredByRequest =
                repository.findByRequestId("tenant-a", "request-1").orElseThrow();

        assertRestored(source, restoredById);
        assertRestored(source, restoredByRequest);
    }

    private void assertSavedHeader(
            PremiumLifecycleAdjustment source, PremiumLifecycleAdjustmentDO savedHeader) {
        assertAll(
                () -> assertEquals(source.getAdjustmentId(), savedHeader.getAdjustmentId()),
                () -> assertEquals(source.getAdjustmentRequestId(), savedHeader.getAdjustmentRequestId()),
                () -> assertEquals(source.getTenantId(), savedHeader.getTenantId()),
                () -> assertEquals(source.getDirection(), savedHeader.getDirection()),
                () -> assertEquals(source.getCustomerAmount(), savedHeader.getCustomerAmount()),
                () -> assertEquals(source.getTaxDirection(), savedHeader.getTaxDirection()),
                () -> assertEquals(source.getTaxAmount(), savedHeader.getTaxAmount()),
                () -> assertEquals(source.getInternalCostDirection(), savedHeader.getInternalCostDirection()),
                () -> assertEquals(source.getInternalCostAmount(), savedHeader.getInternalCostAmount()),
                () -> assertEquals(source.getRequestHash(), savedHeader.getRequestHash()),
                () -> assertEquals(source.getResultHash(), savedHeader.getResultHash()));
    }

    private void assertSavedLines(
            PremiumLifecycleAdjustment source, List<PremiumLifecycleDifferenceLineDO> savedLines) {
        assertEquals(3, savedLines.size());
        assertAll(
                () -> assertEquals(source.getAdjustmentId(), savedLines.getFirst().getId().getAdjustmentId()),
                () -> assertEquals("premium", savedLines.getFirst().getId().getLineId()),
                () -> assertEquals(ChargeDirection.CREDIT, savedLines.getFirst().getDirection()),
                () -> assertEquals(new BigDecimal("20.00"), savedLines.getFirst().getDifferenceAmount()),
                () -> assertEquals("commission", savedLines.getLast().getId().getLineId()),
                () -> assertEquals(ChargeDirection.DEBIT, savedLines.getLast().getDirection()),
                () -> assertEquals(new BigDecimal("5.25"), savedLines.getLast().getDifferenceAmount()));
    }

    private void assertRestored(PremiumLifecycleAdjustment source, PremiumLifecycleAdjustment restored) {
        assertAll(
                () -> assertEquals(source.getAdjustmentId(), restored.getAdjustmentId()),
                () -> assertEquals(source.getAdjustmentRequestId(), restored.getAdjustmentRequestId()),
                () -> assertEquals(source.getBizNo(), restored.getBizNo()),
                () -> assertEquals(source.getLifecycleType(), restored.getLifecycleType()),
                () -> assertEquals(source.getTenantId(), restored.getTenantId()),
                () -> assertEquals(source.getProductId(), restored.getProductId()),
                () -> assertEquals(source.getOriginalCalculationId(), restored.getOriginalCalculationId()),
                () -> assertEquals(source.getOriginalResultHash(), restored.getOriginalResultHash()),
                () -> assertEquals(source.getReplacementCalculationId(), restored.getReplacementCalculationId()),
                () -> assertEquals(source.getReplacementResultHash(), restored.getReplacementResultHash()),
                () -> assertEquals(source.getBusinessTime(), restored.getBusinessTime()),
                () -> assertEquals(source.getCurrency(), restored.getCurrency()),
                () -> assertEquals(PremiumBalanceDirection.CREDIT, restored.getDirection()),
                () -> assertEquals(new BigDecimal("25.50"), restored.getCustomerAmount()),
                () -> assertEquals(PremiumBalanceDirection.CREDIT, restored.getTaxDirection()),
                () -> assertEquals(new BigDecimal("5.50"), restored.getTaxAmount()),
                () -> assertEquals(PremiumBalanceDirection.DEBIT, restored.getInternalCostDirection()),
                () -> assertEquals(new BigDecimal("5.25"), restored.getInternalCostAmount()),
                () -> assertEquals(source.getLines(), restored.getLines()),
                () -> assertEquals(source.getReason(), restored.getReason()),
                () -> assertEquals(source.getRequestHash(), restored.getRequestHash()),
                () -> assertEquals(source.getResultHash(), restored.getResultHash()),
                () -> assertEquals(source.getCreatedAt(), restored.getCreatedAt()));
    }

    private PremiumLifecycleAdjustment adjustment() {
        PremiumLifecycleDifference difference = new PremiumLifecycleDifference(
                PremiumBalanceDirection.CREDIT,
                new BigDecimal("25.50"),
                PremiumBalanceDirection.CREDIT,
                new BigDecimal("5.50"),
                PremiumBalanceDirection.DEBIT,
                new BigDecimal("5.25"),
                List.of(
                        line("premium", ChargeCategory.RISK_PREMIUM, AmountChannel.CUSTOMER_PRICE,
                                ChargePayerType.POLICYHOLDER, ChargeDirection.CREDIT,
                                "PREMIUM_RECEIVABLE", "120.00", "100.00", "20.00", true, true),
                        line("tax", ChargeCategory.TAX, AmountChannel.CUSTOMER_PRICE,
                                ChargePayerType.POLICYHOLDER, ChargeDirection.CREDIT,
                                "TAX_PAYABLE", "12.00", "6.50", "5.50", true, true),
                        line("commission", ChargeCategory.COMMISSION, AmountChannel.INTERNAL_COST,
                                ChargePayerType.CHANNEL, ChargeDirection.DEBIT,
                                "COMMISSION_PAYABLE", "10.00", "15.25", "5.25", false, false)));
        return PremiumLifecycleAdjustment.confirm(
                "adjustment-1", "request-1", "policy-1", PremiumLifecycleType.ENDORSEMENT,
                "tenant-a", "product-1", "calculation-original", hash('o'),
                "calculation-replacement", hash('n'), LocalDateTime.of(2026, 8, 20, 10, 30),
                "CNY", difference, "批改减额", hash('q'), hash('r'),
                LocalDateTime.of(2026, 8, 20, 10, 31));
    }

    private PremiumLifecycleDifferenceLine line(
            String lineId,
            ChargeCategory category,
            AmountChannel amountChannel,
            ChargePayerType payerType,
            ChargeDirection differenceDirection,
            String accountingClass,
            String beforeAmount,
            String afterAmount,
            String differenceAmount,
            boolean customerVisible,
            boolean affectsCustomerPayable) {
        return new PremiumLifecycleDifferenceLine(
                lineId, lineId.toUpperCase(), "V1", "V2", category, amountChannel,
                differenceDirection, payerType, accountingClass, "CNY", ChargeDirection.DEBIT,
                new BigDecimal(beforeAmount), ChargeDirection.DEBIT, new BigDecimal(afterAmount),
                new BigDecimal(differenceAmount), customerVisible, affectsCustomerPayable, lineId + "差额");
    }

    private String hash(char value) {
        return String.valueOf(value).repeat(64);
    }
}
