package com.titanium.product.infrastructure.pricing.adapter.paidup;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.infrastructure.mapper.ReducedPaidUpPolicyPersistenceMapperImpl;
import com.titanium.product.infrastructure.pricing.entity.paidup.ReducedPaidUpPolicyDO;
import com.titanium.product.infrastructure.pricing.repository.paidup.ReducedPaidUpPolicyJpaRepository;
import com.titanium.product.pricing.aggregate.paidup.ReducedPaidUpPolicy;

/**
 * 减额缴清净费率策略关系型仓储适配器测试（g04-05）。
 * <p>
 * 锁定写入链路（草稿落库 + 审批哈希随库）与领域↔持久化往返一致性——
 * 减额缴清核定时按 {@code findPublished} 取到的费率必须与发布时审核过的内容一致，
 * 且费率列（{@code DECIMAL(20,8)}）往返全程不得丢精度。
 * </p>
 */
class JpaReducedPaidUpPolicyRepositoryTest {

    private static final LocalDateTime EFFECTIVE_FROM = LocalDateTime.of(2026, 1, 1, 0, 0);

    private ReducedPaidUpPolicyJpaRepository    jpaRepository;
    private JpaReducedPaidUpPolicyRepository    repository;

    @BeforeEach
    void setUp() {
        jpaRepository = mock(ReducedPaidUpPolicyJpaRepository.class);
        repository = new JpaReducedPaidUpPolicyRepository(
                jpaRepository, new ReducedPaidUpPolicyPersistenceMapperImpl());
    }

    @Test
    void shouldPersistDraftPolicyAndRestoreIt() {
        ReducedPaidUpPolicy policy = draftPolicy();

        repository.save(policy);

        ArgumentCaptor<ReducedPaidUpPolicyDO> captor = ArgumentCaptor.forClass(ReducedPaidUpPolicyDO.class);
        verify(jpaRepository).save(captor.capture());
        ReducedPaidUpPolicyDO saved = captor.getValue();
        assertAll(
                () -> assertEquals("paidup-1", saved.getPolicyId()),
                () -> assertEquals("product-1", saved.getProductId()),
                () -> assertEquals("PAIDUP-2026", saved.getPolicyCode()),
                () -> assertEquals("V1", saved.getPolicyVersion()),
                () -> assertEquals(0, new BigDecimal("0.60").compareTo(saved.getNetSinglePremiumRatePerUnit())),
                () -> assertEquals(EFFECTIVE_FROM, saved.getEffectiveFrom()),
                () -> assertEquals("tenant-a", saved.getTenantId()),
                () -> assertEquals(ActuarialDefinitionStatus.DRAFT, saved.getStatus()));

        when(jpaRepository.findByPolicyIdAndTenantIdAndProductId("paidup-1", "tenant-a", "product-1"))
                .thenReturn(Optional.of(saved));
        ReducedPaidUpPolicy restored = repository.findById("tenant-a", "product-1", "paidup-1").orElseThrow();
        assertAll(
                () -> assertEquals(policy.getPolicyId(), restored.getPolicyId()),
                () -> assertEquals(policy.getPolicyCode(), restored.getPolicyCode()),
                () -> assertEquals(0, policy.getNetSinglePremiumRatePerUnit()
                        .compareTo(restored.getNetSinglePremiumRatePerUnit())),
                () -> assertEquals(ActuarialDefinitionStatus.DRAFT, restored.getStatus()));
    }

    @Test
    void shouldRoundTripRateGreaterThanOneWithoutLoss() {
        // 费率非比例：上界不存在，往返必须逐位保真（DECIMAL(20,8)）
        ReducedPaidUpPolicy policy = ReducedPaidUpPolicy.createDraft(
                "paidup-1", "product-1", "PAIDUP-2026", "V1", new BigDecimal("1.25000000"),
                EFFECTIVE_FROM, null, "tenant-a");

        repository.save(policy);

        ArgumentCaptor<ReducedPaidUpPolicyDO> captor = ArgumentCaptor.forClass(ReducedPaidUpPolicyDO.class);
        verify(jpaRepository).save(captor.capture());
        assertEquals(0, new BigDecimal("1.25").compareTo(captor.getValue().getNetSinglePremiumRatePerUnit()));
    }

    @Test
    void shouldPersistContentHashProducedByApproval() {
        ReducedPaidUpPolicy policy = draftPolicy();
        String contentHash = policy.approve();

        repository.save(policy);

        ArgumentCaptor<ReducedPaidUpPolicyDO> captor = ArgumentCaptor.forClass(ReducedPaidUpPolicyDO.class);
        verify(jpaRepository).save(captor.capture());
        ReducedPaidUpPolicyDO saved = captor.getValue();
        assertEquals(ActuarialDefinitionStatus.APPROVED, saved.getStatus());
        assertEquals(contentHash, saved.getContentHash());
        assertEquals(64, saved.getContentHash().length());
    }

    @Test
    void shouldDelegateBusinessKeyExistenceCheck() {
        when(jpaRepository.existsByTenantIdAndProductIdAndPolicyCodeAndPolicyVersion(
                "tenant-a", "product-1", "PAIDUP-2026", "V1")).thenReturn(true);
        when(jpaRepository.existsByTenantIdAndProductIdAndPolicyCodeAndPolicyVersion(
                "tenant-a", "product-1", "PAIDUP-2026", "V2")).thenReturn(false);

        assertTrue(repository.existsByBusinessKey("tenant-a", "product-1", "PAIDUP-2026", "V1"));
        assertFalse(repository.existsByBusinessKey("tenant-a", "product-1", "PAIDUP-2026", "V2"));
    }

    @Test
    void shouldReturnEmptyWhenPolicyAbsent() {
        when(jpaRepository.findByPolicyIdAndTenantIdAndProductId(any(), any(), any())).thenReturn(Optional.empty());

        assertTrue(repository.findById("tenant-a", "product-1", "missing").isEmpty());
    }

    /** 构建草稿态减额缴清策略（与新增链路 createDraft 同参）。 */
    private ReducedPaidUpPolicy draftPolicy() {
        return ReducedPaidUpPolicy.createDraft(
                "paidup-1", "product-1", "PAIDUP-2026", "V1", new BigDecimal("0.60"),
                EFFECTIVE_FROM, null, "tenant-a");
    }
}
