package com.titanium.product.infrastructure.pricing.adapter.loan;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.titanium.product.infrastructure.mapper.PolicyLoanPolicyPersistenceMapperImpl;
import com.titanium.product.infrastructure.pricing.entity.loan.PolicyLoanPolicyDO;
import com.titanium.product.infrastructure.pricing.repository.loan.PolicyLoanPolicyJpaRepository;
import com.titanium.product.pricing.aggregate.loan.PolicyLoanPolicy;

/**
 * 保单贷款策略关系型仓储适配器测试（g04-03）。
 * <p>
 * 锁定写入链路（草稿落库 + 审批哈希随库）与领域↔持久化往返一致性——
 * 放款时按 {@code findPublished} 取到的比例/利率必须与发布时审核过的内容一致。
 * </p>
 */
class JpaPolicyLoanPolicyRepositoryTest {

    private PolicyLoanPolicyJpaRepository    jpaRepository;
    private JpaPolicyLoanPolicyRepository    repository;

    private static final LocalDateTime EFFECTIVE_FROM = LocalDateTime.of(2026, 1, 1, 0, 0);

    @BeforeEach
    void setUp() {
        jpaRepository = mock(PolicyLoanPolicyJpaRepository.class);
        repository = new JpaPolicyLoanPolicyRepository(
                jpaRepository, new PolicyLoanPolicyPersistenceMapperImpl());
    }

    @Test
    void shouldPersistDraftPolicyAndRestoreIt() {
        PolicyLoanPolicy policy = draftPolicy();

        repository.save(policy);

        ArgumentCaptor<PolicyLoanPolicyDO> captor = ArgumentCaptor.forClass(PolicyLoanPolicyDO.class);
        verify(jpaRepository).save(captor.capture());
        PolicyLoanPolicyDO saved = captor.getValue();
        assertAll(
                () -> assertEquals("loan-1", saved.getPolicyId()),
                () -> assertEquals("product-1", saved.getProductId()),
                () -> assertEquals("LOAN-2026", saved.getPolicyCode()),
                () -> assertEquals("V1", saved.getPolicyVersion()),
                () -> assertEquals(0, new BigDecimal("0.80").compareTo(saved.getLoanableRatio())),
                () -> assertEquals(0, new BigDecimal("0.05").compareTo(saved.getInterestRate())),
                () -> assertEquals(EFFECTIVE_FROM, saved.getEffectiveFrom()),
                () -> assertEquals("tenant-a", saved.getTenantId()),
                () -> assertEquals(ActuarialDefinitionStatus.DRAFT, saved.getStatus()));

        when(jpaRepository.findByPolicyIdAndTenantIdAndProductId("loan-1", "tenant-a", "product-1"))
                .thenReturn(Optional.of(saved));
        PolicyLoanPolicy restored = repository.findById("tenant-a", "product-1", "loan-1").orElseThrow();
        assertAll(
                () -> assertEquals(policy.getPolicyId(), restored.getPolicyId()),
                () -> assertEquals(policy.getPolicyCode(), restored.getPolicyCode()),
                () -> assertEquals(0, policy.getLoanableRatio().compareTo(restored.getLoanableRatio())),
                () -> assertEquals(0, policy.getInterestRate().compareTo(restored.getInterestRate())),
                () -> assertEquals(ActuarialDefinitionStatus.DRAFT, restored.getStatus()));
    }

    @Test
    void shouldPersistContentHashProducedByApproval() {
        PolicyLoanPolicy policy = draftPolicy();
        String contentHash = policy.approve();

        repository.save(policy);

        ArgumentCaptor<PolicyLoanPolicyDO> captor = ArgumentCaptor.forClass(PolicyLoanPolicyDO.class);
        verify(jpaRepository).save(captor.capture());
        PolicyLoanPolicyDO saved = captor.getValue();
        assertEquals(ActuarialDefinitionStatus.APPROVED, saved.getStatus());
        assertEquals(contentHash, saved.getContentHash());
        assertTrue(saved.getContentHash().length() == 64);
    }

    @Test
    void shouldDelegateBusinessKeyExistenceCheck() {
        when(jpaRepository.existsByTenantIdAndProductIdAndPolicyCodeAndPolicyVersion(
                "tenant-a", "product-1", "LOAN-2026", "V1")).thenReturn(true);
        when(jpaRepository.existsByTenantIdAndProductIdAndPolicyCodeAndPolicyVersion(
                "tenant-a", "product-1", "LOAN-2026", "V2")).thenReturn(false);

        assertTrue(repository.existsByBusinessKey("tenant-a", "product-1", "LOAN-2026", "V1"));
        assertTrue(!repository.existsByBusinessKey("tenant-a", "product-1", "LOAN-2026", "V2"));
    }

    @Test
    void shouldReturnEmptyWhenPolicyAbsent() {
        when(jpaRepository.findByPolicyIdAndTenantIdAndProductId(any(), any(), any())).thenReturn(Optional.empty());

        assertTrue(repository.findById("tenant-a", "product-1", "missing").isEmpty());
    }

    /** 构建草稿态贷款策略（与新增链路 createDraft 同参）。 */
    private PolicyLoanPolicy draftPolicy() {
        return PolicyLoanPolicy.createDraft(
                "loan-1", "product-1", "LOAN-2026", "V1", new BigDecimal("0.80"), new BigDecimal("0.05"),
                EFFECTIVE_FROM, null, "tenant-a");
    }
}
