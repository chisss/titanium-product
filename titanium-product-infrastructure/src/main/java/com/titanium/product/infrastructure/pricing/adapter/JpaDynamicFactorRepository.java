package com.titanium.product.infrastructure.pricing.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.product.aggregate.DynamicFactorDefinition;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.infrastructure.pricing.entity.DynamicFactorEntity;
import com.titanium.product.infrastructure.pricing.repository.DynamicFactorJpaRepository;
import com.titanium.product.repository.DynamicFactorRepository;

import lombok.RequiredArgsConstructor;

/** 动态因子关系型仓储适配器。 */
@Repository
@RequiredArgsConstructor
public class JpaDynamicFactorRepository implements DynamicFactorRepository {

    private final DynamicFactorJpaRepository jpaRepository;

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
        List<DynamicFactorEntity> entities = status == null
                ? jpaRepository.findByTenantIdAndProductIdOrderByFactorCodeAscFactorVersionDesc(tenantId, productId)
                : jpaRepository.findByTenantIdAndProductIdAndStatusOrderByFactorCodeAscFactorVersionDesc(
                        tenantId, productId, status);
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void save(DynamicFactorDefinition factor) {
        jpaRepository.save(toEntity(factor));
    }

    private DynamicFactorDefinition toDomain(DynamicFactorEntity entity) {
        return DynamicFactorDefinition.restore(
                entity.getFactorId(), entity.getProductId(), entity.getFactorCode(), entity.getFactorVersion(),
                entity.getFactorName(), entity.getDescription(), entity.getFeatureCode(),
                entity.getFeatureDefinitionVersion(), entity.getSourceType(), entity.getValueTimePolicy(),
                entity.getLowerBound(), entity.getUpperBound(), entity.getMissingPolicy(), entity.getDefaultValue(),
                entity.getTransformType(), entity.getMultiplier(), entity.getOffset(), entity.isReplayable(),
                entity.getEffectiveFrom(), entity.getEffectiveTo(), entity.getTenantId(), entity.getStatus(),
                entity.getContentHash());
    }

    private DynamicFactorEntity toEntity(DynamicFactorDefinition factor) {
        DynamicFactorEntity entity = new DynamicFactorEntity();
        entity.setFactorId(factor.getFactorId());
        entity.setProductId(factor.getProductId());
        entity.setFactorCode(factor.getFactorCode());
        entity.setFactorVersion(factor.getFactorVersion());
        entity.setFactorName(factor.getFactorName());
        entity.setDescription(factor.getDescription());
        entity.setFeatureCode(factor.getFeatureCode());
        entity.setFeatureDefinitionVersion(factor.getFeatureDefinitionVersion());
        entity.setSourceType(factor.getSourceType());
        entity.setValueTimePolicy(factor.getValueTimePolicy());
        entity.setLowerBound(factor.getLowerBound());
        entity.setUpperBound(factor.getUpperBound());
        entity.setMissingPolicy(factor.getMissingPolicy());
        entity.setDefaultValue(factor.getDefaultValue());
        entity.setTransformType(factor.getTransformType());
        entity.setMultiplier(factor.getMultiplier());
        entity.setOffset(factor.getOffset());
        entity.setReplayable(factor.isReplayable());
        entity.setEffectiveFrom(factor.getEffectiveFrom());
        entity.setEffectiveTo(factor.getEffectiveTo());
        entity.setTenantId(factor.getTenantId());
        entity.setStatus(factor.getStatus());
        entity.setContentHash(factor.getContentHash());
        return entity;
    }
}
