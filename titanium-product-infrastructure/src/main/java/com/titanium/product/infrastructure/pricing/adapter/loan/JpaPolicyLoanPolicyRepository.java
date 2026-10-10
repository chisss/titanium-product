package com.titanium.product.infrastructure.pricing.adapter.loan;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.product.infrastructure.mapper.PolicyLoanPolicyPersistenceMapper;
import com.titanium.product.infrastructure.pricing.entity.loan.PolicyLoanPolicyDO;
import com.titanium.product.infrastructure.pricing.repository.loan.PolicyLoanPolicyJpaRepository;
import com.titanium.product.pricing.aggregate.loan.PolicyLoanPolicy;
import com.titanium.product.repository.PolicyLoanPolicyRepository;

import lombok.RequiredArgsConstructor;

/** 保单贷款策略关系型仓储适配器（g04-03）。 */
@Repository
@RequiredArgsConstructor
public class JpaPolicyLoanPolicyRepository implements PolicyLoanPolicyRepository {

    private final PolicyLoanPolicyJpaRepository     jpaRepository;
    private final PolicyLoanPolicyPersistenceMapper persistenceMapper;

    @Override
    public boolean existsByBusinessKey(
            String tenantId, String productId, String policyCode, String policyVersion) {
        return jpaRepository.existsByTenantIdAndProductIdAndPolicyCodeAndPolicyVersion(
                tenantId, productId, policyCode, policyVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PolicyLoanPolicy> findById(String tenantId, String productId, String policyId) {
        return jpaRepository.findByPolicyIdAndTenantIdAndProductId(policyId, tenantId, productId)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PolicyLoanPolicy> findPublished(
            String tenantId, String productId, LocalDateTime businessTime) {
        return jpaRepository.findPublished(tenantId, productId, businessTime).map(this::toDomain);
    }

    @Override
    @Transactional
    public void save(PolicyLoanPolicy policy) {
        jpaRepository.save(persistenceMapper.toDO(policy));
    }

    private PolicyLoanPolicy toDomain(PolicyLoanPolicyDO entity) {
        return PolicyLoanPolicy.restore(
                entity.getPolicyId(), entity.getProductId(), entity.getPolicyCode(), entity.getPolicyVersion(),
                entity.getLoanableRatio(), entity.getInterestRate(), entity.getEffectiveFrom(),
                entity.getEffectiveTo(), entity.getTenantId(), entity.getStatus(), entity.getContentHash());
    }
}
