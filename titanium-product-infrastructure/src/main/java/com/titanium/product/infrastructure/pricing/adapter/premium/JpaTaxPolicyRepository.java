package com.titanium.product.infrastructure.pricing.adapter.premium;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.infrastructure.mapper.TaxPolicyPersistenceMapper;
import com.titanium.product.infrastructure.pricing.entity.premium.TaxBaseItemDO;
import com.titanium.product.infrastructure.pricing.entity.premium.TaxPolicyDO;
import com.titanium.product.infrastructure.pricing.repository.premium.TaxBaseItemJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.premium.TaxPolicyJpaRepository;
import com.titanium.product.pricing.aggregate.TaxPolicyDefinition;
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
    private final TaxPolicyPersistenceMapper persistenceMapper;

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
        List<TaxPolicyDO> dataObjects = status == null
                ? policyRepository.findByTenantIdAndProductIdOrderByPolicyCodeAscPolicyVersionDesc(tenantId, productId)
                : policyRepository.findByTenantIdAndProductIdAndStatusOrderByPolicyCodeAscPolicyVersionDesc(
                        tenantId, productId, status);
        return dataObjects.stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void save(TaxPolicyDefinition policy) {
        boolean existing = policyRepository.existsById(policy.getPolicyId());
        policyRepository.save(persistenceMapper.toDO(policy));
        if (!existing) {
            baseItemRepository.saveAll(toBaseItems(policy));
        }
    }

    private TaxPolicyDefinition toDomain(TaxPolicyDO dataObject) {
        List<String> baseCodes = baseItemRepository.findByPolicyIdOrderBySortOrderAsc(dataObject.getPolicyId())
                .stream()
                .map(TaxBaseItemDO::getComponentCode)
                .toList();
        return TaxPolicyDefinition.restore(
                dataObject.getPolicyId(), dataObject.getProductId(), dataObject.getPolicyCode(),
                dataObject.getPolicyVersion(), dataObject.getPolicyName(), dataObject.getDescription(),
                dataObject.getJurisdictionCode(), dataObject.getCategory(), dataObject.getPayerType(),
                dataObject.getPriceMode(), dataObject.getTaxRate(), baseCodes,
                dataObject.getAccountingClass(), dataObject.getRegulatoryReferenceId(),
                dataObject.getExemptionFeatureCode(), dataObject.getEffectiveFrom(), dataObject.getEffectiveTo(),
                dataObject.getTenantId(), dataObject.getStatus(), dataObject.getContentHash());
    }

    private List<TaxBaseItemDO> toBaseItems(TaxPolicyDefinition policy) {
        return IntStream.range(0, policy.getBaseComponentCodes().size())
                .mapToObj(index -> {
                    String code = policy.getBaseComponentCodes().get(index);
                    String itemId = UUID.nameUUIDFromBytes(
                            (policy.getPolicyId() + ':' + code).getBytes(StandardCharsets.UTF_8)).toString();
                    return persistenceMapper.toDO(policy, itemId, code, index);
                })
                .toList();
    }
}
