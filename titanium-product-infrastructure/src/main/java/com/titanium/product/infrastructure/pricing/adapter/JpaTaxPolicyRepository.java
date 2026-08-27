package com.titanium.product.infrastructure.pricing.adapter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.product.aggregate.TaxPolicyDefinition;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.infrastructure.pricing.entity.TaxBaseItemEntity;
import com.titanium.product.infrastructure.pricing.entity.TaxPolicyEntity;
import com.titanium.product.infrastructure.pricing.repository.TaxBaseItemJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.TaxPolicyJpaRepository;
import com.titanium.product.repository.TaxPolicyRepository;

import lombok.RequiredArgsConstructor;

/**
 * 税费策略关系型仓储适配器。
 */
@Repository
@RequiredArgsConstructor
public class JpaTaxPolicyRepository implements TaxPolicyRepository {

    private final TaxPolicyJpaRepository policyRepository;
    private final TaxBaseItemJpaRepository baseItemRepository;

    @Override
    public boolean existsByBusinessKey(String tenantId, String productId, String policyCode, String policyVersion) {
        return policyRepository.existsByTenantIdAndProductIdAndPolicyCodeAndPolicyVersion(
                tenantId, productId, policyCode, policyVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TaxPolicyDefinition> findById(String tenantId, String productId, String policyId) {
        return policyRepository.findByPolicyIdAndTenantIdAndProductId(policyId, tenantId, productId)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TaxPolicyDefinition> findPublished(
            String tenantId, String productId, String policyCode, String policyVersion, LocalDateTime businessTime) {
        return policyRepository.findPublished(tenantId, productId, policyCode, policyVersion, businessTime)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaxPolicyDefinition> findAll(
            String tenantId, String productId, ActuarialDefinitionStatus status) {
        List<TaxPolicyEntity> entities = status == null
                ? policyRepository.findByTenantIdAndProductIdOrderByPolicyCodeAscPolicyVersionDesc(tenantId, productId)
                : policyRepository.findByTenantIdAndProductIdAndStatusOrderByPolicyCodeAscPolicyVersionDesc(
                        tenantId, productId, status);
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void save(TaxPolicyDefinition policy) {
        boolean existing = policyRepository.existsById(policy.getPolicyId());
        policyRepository.save(toEntity(policy));
        if (!existing) {
            baseItemRepository.saveAll(toBaseItems(policy));
        }
    }

    private TaxPolicyDefinition toDomain(TaxPolicyEntity entity) {
        List<String> baseCodes = baseItemRepository.findByPolicyIdOrderBySortOrderAsc(entity.getPolicyId()).stream()
                .map(TaxBaseItemEntity::getComponentCode)
                .toList();
        return TaxPolicyDefinition.restore(
                entity.getPolicyId(), entity.getProductId(), entity.getPolicyCode(), entity.getPolicyVersion(),
                entity.getPolicyName(), entity.getDescription(), entity.getJurisdictionCode(), entity.getCategory(),
                entity.getPayerType(), entity.getPriceMode(), entity.getTaxRate(), baseCodes,
                entity.getAccountingClass(), entity.getRegulatoryReferenceId(), entity.getExemptionFeatureCode(),
                entity.getEffectiveFrom(), entity.getEffectiveTo(), entity.getTenantId(), entity.getStatus(),
                entity.getContentHash());
    }

    private TaxPolicyEntity toEntity(TaxPolicyDefinition policy) {
        TaxPolicyEntity entity = new TaxPolicyEntity();
        entity.setPolicyId(policy.getPolicyId());
        entity.setProductId(policy.getProductId());
        entity.setPolicyCode(policy.getPolicyCode());
        entity.setPolicyVersion(policy.getPolicyVersion());
        entity.setPolicyName(policy.getPolicyName());
        entity.setDescription(policy.getDescription());
        entity.setJurisdictionCode(policy.getJurisdictionCode());
        entity.setCategory(policy.getCategory());
        entity.setPayerType(policy.getPayerType());
        entity.setPriceMode(policy.getPriceMode());
        entity.setTaxRate(policy.getTaxRate());
        entity.setAccountingClass(policy.getAccountingClass());
        entity.setRegulatoryReferenceId(policy.getRegulatoryReferenceId());
        entity.setExemptionFeatureCode(policy.getExemptionFeatureCode());
        entity.setEffectiveFrom(policy.getEffectiveFrom());
        entity.setEffectiveTo(policy.getEffectiveTo());
        entity.setTenantId(policy.getTenantId());
        entity.setStatus(policy.getStatus());
        entity.setContentHash(policy.getContentHash());
        return entity;
    }

    private List<TaxBaseItemEntity> toBaseItems(TaxPolicyDefinition policy) {
        return java.util.stream.IntStream.range(0, policy.getBaseComponentCodes().size())
                .mapToObj(index -> {
                    String code = policy.getBaseComponentCodes().get(index);
                    TaxBaseItemEntity entity = new TaxBaseItemEntity();
                    entity.setItemId(UUID.nameUUIDFromBytes(
                            (policy.getPolicyId() + ':' + code).getBytes(StandardCharsets.UTF_8)).toString());
                    entity.setPolicyId(policy.getPolicyId());
                    entity.setComponentCode(code);
                    entity.setSortOrder(index);
                    return entity;
                })
                .toList();
    }
}
