package com.titanium.product.infrastructure.pricing.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.product.aggregate.DynamicFactorDefinition;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.infrastructure.mapper.DynamicFactorPersistenceMapper;
import com.titanium.product.infrastructure.pricing.entity.DynamicFactorDO;
import com.titanium.product.infrastructure.pricing.repository.DynamicFactorJpaRepository;
import com.titanium.product.repository.DynamicFactorRepository;

import lombok.RequiredArgsConstructor;

/** 动态因子关系型仓储适配器。 */
@Repository
@RequiredArgsConstructor
public class JpaDynamicFactorRepository implements DynamicFactorRepository {

    private final DynamicFactorJpaRepository jpaRepository;
    private final DynamicFactorPersistenceMapper persistenceMapper;

    @Override
    public boolean existsByBusinessKey(
            String tenantId, String productId, String factorCode, String factorVersion) {
        return jpaRepository.existsByTenantIdAndProductIdAndFactorCodeAndFactorVersion(
                tenantId, productId, factorCode, factorVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DynamicFactorDefinition> findById(String tenantId, String productId, String factorId) {
        return jpaRepository.findByFactorIdAndTenantIdAndProductId(factorId, tenantId, productId)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DynamicFactorDefinition> findPublished(
            String tenantId, String productId, String factorCode, String factorVersion,
            LocalDateTime businessTime) {
        return jpaRepository.findPublished(tenantId, productId, factorCode, factorVersion, businessTime)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DynamicFactorDefinition> findAll(
            String tenantId, String productId, ActuarialDefinitionStatus status) {
        List<DynamicFactorDO> dataObjects = status == null
                ? jpaRepository.findByTenantIdAndProductIdOrderByFactorCodeAscFactorVersionDesc(tenantId, productId)
                : jpaRepository.findByTenantIdAndProductIdAndStatusOrderByFactorCodeAscFactorVersionDesc(
                        tenantId, productId, status);
        return dataObjects.stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void save(DynamicFactorDefinition factor) {
        jpaRepository.save(persistenceMapper.toDO(factor));
    }

    private DynamicFactorDefinition toDomain(DynamicFactorDO dataObject) {
        return DynamicFactorDefinition.restore(
                dataObject.getFactorId(), dataObject.getProductId(), dataObject.getFactorCode(),
                dataObject.getFactorVersion(), dataObject.getFactorName(), dataObject.getDescription(),
                dataObject.getFeatureCode(), dataObject.getFeatureDefinitionVersion(), dataObject.getSourceType(),
                dataObject.getValueTimePolicy(), dataObject.getLowerBound(), dataObject.getUpperBound(),
                dataObject.getMissingPolicy(), dataObject.getDefaultValue(), dataObject.getTransformType(),
                dataObject.getMultiplier(), dataObject.getOffset(), dataObject.isReplayable(),
                dataObject.getEffectiveFrom(), dataObject.getEffectiveTo(), dataObject.getTenantId(),
                dataObject.getStatus(), dataObject.getContentHash());
    }
}
