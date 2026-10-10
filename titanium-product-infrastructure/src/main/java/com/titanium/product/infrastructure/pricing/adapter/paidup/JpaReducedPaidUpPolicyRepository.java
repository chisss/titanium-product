package com.titanium.product.infrastructure.pricing.adapter.paidup;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.product.infrastructure.mapper.ReducedPaidUpPolicyPersistenceMapper;
import com.titanium.product.infrastructure.pricing.entity.paidup.ReducedPaidUpPolicyDO;
import com.titanium.product.infrastructure.pricing.repository.paidup.ReducedPaidUpPolicyJpaRepository;
import com.titanium.product.pricing.aggregate.paidup.ReducedPaidUpPolicy;
import com.titanium.product.repository.ReducedPaidUpPolicyRepository;

import lombok.RequiredArgsConstructor;

/** 减额缴清净费率策略关系型仓储适配器（g04-05）。 */
@Repository
@RequiredArgsConstructor
public class JpaReducedPaidUpPolicyRepository implements ReducedPaidUpPolicyRepository {

    private final ReducedPaidUpPolicyJpaRepository     jpaRepository;
    private final ReducedPaidUpPolicyPersistenceMapper persistenceMapper;

    @Override
    public boolean existsByBusinessKey(
            String tenantId, String productId, String policyCode, String policyVersion) {
        return jpaRepository.existsByTenantIdAndProductIdAndPolicyCodeAndPolicyVersion(
                tenantId, productId, policyCode, policyVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReducedPaidUpPolicy> findById(String tenantId, String productId, String policyId) {
        return jpaRepository.findByPolicyIdAndTenantIdAndProductId(policyId, tenantId, productId)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReducedPaidUpPolicy> findPublished(
            String tenantId, String productId, LocalDateTime businessTime) {
        return jpaRepository.findPublished(tenantId, productId, businessTime).map(this::toDomain);
    }

    @Override
    @Transactional
    public void save(ReducedPaidUpPolicy policy) {
        jpaRepository.save(persistenceMapper.toDO(policy));
    }

    private ReducedPaidUpPolicy toDomain(ReducedPaidUpPolicyDO entity) {
        return ReducedPaidUpPolicy.restore(
                entity.getPolicyId(), entity.getProductId(), entity.getPolicyCode(), entity.getPolicyVersion(),
                entity.getNetSinglePremiumRatePerUnit(), entity.getEffectiveFrom(),
                entity.getEffectiveTo(), entity.getTenantId(), entity.getStatus(), entity.getContentHash());
    }
}
