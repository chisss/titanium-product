package com.titanium.product.infrastructure.pricing.adapter.premium;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.common.enums.SurrenderRefundBasis;
import com.titanium.product.infrastructure.mapper.SurrenderValuePolicyPersistenceMapperImpl;
import com.titanium.product.infrastructure.pricing.entity.premium.SurrenderValuePolicyDO;
import com.titanium.product.infrastructure.pricing.repository.premium.SurrenderValuePolicyJpaRepository;
import com.titanium.product.pricing.aggregate.surrender.SurrenderValuePolicy;

/**
 * 退保价值策略关系型仓储适配器测试。
 * <p>
 * 重点回归 D5 断链：该仓储此前<b>只有 {@code findPublished} 无 {@code save}</b>，
 * 聚合的 {@code createDraft/approve/publish/retire} 产物无处落库，策略永远无法录入。
 * 本测试锁定写入链路与领域↔持久化往返一致性。
 * </p>
 */
class JpaSurrenderValuePolicyRepositoryTest {

    private SurrenderValuePolicyJpaRepository  jpaRepository;
    private JpaSurrenderValuePolicyRepository  repository;

    private static final LocalDateTime EFFECTIVE_FROM = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final LocalDateTime EFFECTIVE_TO   = LocalDateTime.of(2027, 1, 1, 0, 0);

    @BeforeEach
    void setUp() {
        jpaRepository = mock(SurrenderValuePolicyJpaRepository.class);
        repository = new JpaSurrenderValuePolicyRepository(
                jpaRepository, new SurrenderValuePolicyPersistenceMapperImpl());
    }

    @Test
    @DisplayName("新增草稿策略落库并可原样还原")
    void shouldPersistDraftPolicyAndRestoreIt() {
        SurrenderValuePolicy policy = draftPolicy();

        repository.save(policy);

        ArgumentCaptor<SurrenderValuePolicyDO> captor = ArgumentCaptor.forClass(SurrenderValuePolicyDO.class);
        verify(jpaRepository).save(captor.capture());
        SurrenderValuePolicyDO saved = captor.getValue();
        assertAll(
                () -> assertEquals("surrender-1", saved.getPolicyId()),
                () -> assertEquals("product-1", saved.getProductId()),
                () -> assertEquals("SV-2026", saved.getPolicyCode()),
                () -> assertEquals("V1", saved.getPolicyVersion()),
                () -> assertEquals(3, saved.getPolicyYear()),
                () -> assertEquals(15, saved.getCoolingOffDays()),
                () -> assertEquals(0, new BigDecimal("0.85").compareTo(saved.getCashValueRate())),
                () -> assertEquals(0, new BigDecimal("0.30").compareTo(saved.getInternalCostRetentionRate())),
                () -> assertEquals(0, new BigDecimal("0.05").compareTo(saved.getHandlingFeeRate())),
                () -> assertEquals(EFFECTIVE_FROM, saved.getEffectiveFrom()),
                () -> assertEquals(EFFECTIVE_TO, saved.getEffectiveTo()),
                () -> assertEquals("tenant-a", saved.getTenantId()),
                () -> assertEquals(ActuarialDefinitionStatus.DRAFT, saved.getStatus()));

        when(jpaRepository.findByPolicyIdAndTenantIdAndProductId("surrender-1", "tenant-a", "product-1"))
                .thenReturn(Optional.of(saved));
        SurrenderValuePolicy restored = repository.findById("tenant-a", "product-1", "surrender-1").orElseThrow();
        assertAll(
                () -> assertEquals(policy.getPolicyId(), restored.getPolicyId()),
                () -> assertEquals(policy.getPolicyCode(), restored.getPolicyCode()),
                () -> assertEquals(policy.getPolicyYear(), restored.getPolicyYear()),
                () -> assertEquals(policy.getCoolingOffDays(), restored.getCoolingOffDays()),
                () -> assertEquals(0, policy.getCashValueRate().compareTo(restored.getCashValueRate())),
                () -> assertEquals(0,
                        policy.getInternalCostRetentionRate().compareTo(restored.getInternalCostRetentionRate())),
                () -> assertEquals(ActuarialDefinitionStatus.DRAFT, restored.getStatus()));
    }

    @Test
    @DisplayName("审批产生的内容哈希随策略落库（生命周期产物不再只存在内存）")
    void shouldPersistContentHashProducedByApproval() {
        SurrenderValuePolicy policy = draftPolicy();
        String contentHash = policy.approve();

        repository.save(policy);

        ArgumentCaptor<SurrenderValuePolicyDO> captor = ArgumentCaptor.forClass(SurrenderValuePolicyDO.class);
        verify(jpaRepository).save(captor.capture());
        SurrenderValuePolicyDO saved = captor.getValue();
        assertEquals(ActuarialDefinitionStatus.APPROVED, saved.getStatus());
        assertEquals(contentHash, saved.getContentHash());
        assertTrue(saved.getContentHash().length() == 64);
    }

    @Test
    @DisplayName("按业务键判断策略是否已存在，供创建前查重")
    void shouldDelegateBusinessKeyExistenceCheck() {
        when(jpaRepository.existsByTenantIdAndProductIdAndPolicyCodeAndPolicyVersionAndPolicyYear(
                "tenant-a", "product-1", "SV-2026", "V1", 3)).thenReturn(true);
        when(jpaRepository.existsByTenantIdAndProductIdAndPolicyCodeAndPolicyVersionAndPolicyYear(
                "tenant-a", "product-1", "SV-2026", "V1", 4)).thenReturn(false);

        assertTrue(repository.existsByBusinessKey("tenant-a", "product-1", "SV-2026", "V1", 3));
        // 同一编码版本的相邻保单年度是合法共存的两条策略，不得误判为重复
        assertTrue(!repository.existsByBusinessKey("tenant-a", "product-1", "SV-2026", "V1", 4));
    }

    @Test
    @DisplayName("按ID查询为空时返回空 Optional 而非抛异常")
    void shouldReturnEmptyWhenPolicyAbsent() {
        when(jpaRepository.findByPolicyIdAndTenantIdAndProductId(any(), any(), any())).thenReturn(Optional.empty());

        assertTrue(repository.findById("tenant-a", "product-1", "missing").isEmpty());
    }

    /** 构建草稿态退保价值策略（与新增链路 createDraft 同参）。手续费率取非空值以覆盖该列的持久化往返。 */
    private SurrenderValuePolicy draftPolicy() {
        return SurrenderValuePolicy.createDraft(
                "surrender-1", "product-1", "SV-2026", "V1", 3, 15,
                new BigDecimal("0.85"), new BigDecimal("0.30"), new BigDecimal("0.05"),
                SurrenderRefundBasis.CASH_VALUE,
                EFFECTIVE_FROM, EFFECTIVE_TO, "tenant-a");
    }
}
