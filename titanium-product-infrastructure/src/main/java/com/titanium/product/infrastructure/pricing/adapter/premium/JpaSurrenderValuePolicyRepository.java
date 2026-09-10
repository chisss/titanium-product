package com.titanium.product.infrastructure.pricing.adapter.premium;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.product.infrastructure.mapper.SurrenderValuePolicyPersistenceMapper;
import com.titanium.product.infrastructure.pricing.entity.premium.SurrenderValuePolicyDO;
import com.titanium.product.infrastructure.pricing.repository.premium.SurrenderValuePolicyJpaRepository;
import com.titanium.product.pricing.aggregate.surrender.SurrenderValuePolicy;
import com.titanium.product.repository.SurrenderValuePolicyRepository;

import lombok.RequiredArgsConstructor;

/** 退保价值策略关系型仓储适配器。 */
@Repository
@RequiredArgsConstructor
public class JpaSurrenderValuePolicyRepository implements SurrenderValuePolicyRepository {

    private final SurrenderValuePolicyJpaRepository          jpaRepository;
    private final SurrenderValuePolicyPersistenceMapper      persistenceMapper;

    @Override
    public boolean existsByBusinessKey(
            String tenantId, String productId, String policyCode, String policyVersion, int policyYear) {
        return jpaRepository.existsByTenantIdAndProductIdAndPolicyCodeAndPolicyVersionAndPolicyYear(
                tenantId, productId, policyCode, policyVersion, policyYear);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SurrenderValuePolicy> findById(String tenantId, String productId, String policyId) {
        return jpaRepository.findByPolicyIdAndTenantIdAndProductId(policyId, tenantId, productId)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SurrenderValuePolicy> findPublished(
            String tenantId, String productId, int policyYear, LocalDateTime businessTime) {
        return jpaRepository.findPublished(tenantId, productId, policyYear, businessTime).map(this::toDomain);
    }

    @Override
    @Transactional
    public void save(SurrenderValuePolicy policy) {
        jpaRepository.save(persistenceMapper.toDO(policy));
    }

    private SurrenderValuePolicy toDomain(SurrenderValuePolicyDO entity) {
        return SurrenderValuePolicy.restore(
                entity.getPolicyId(), entity.getProductId(), entity.getPolicyCode(), entity.getPolicyVersion(),
                entity.getPolicyYear(), entity.getCoolingOffDays(), entity.getCashValueRate(),
                entity.getInternalCostRetentionRate(), entity.getEffectiveFrom(), entity.getEffectiveTo(),
                entity.getTenantId(), entity.getStatus(), entity.getContentHash());
    }
}
