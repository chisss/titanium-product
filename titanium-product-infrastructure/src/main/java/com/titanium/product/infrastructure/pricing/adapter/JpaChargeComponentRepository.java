package com.titanium.product.infrastructure.pricing.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.titanium.product.aggregate.ChargeComponentDefinition;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.infrastructure.pricing.entity.ChargeComponentEntity;
import com.titanium.product.infrastructure.pricing.repository.ChargeComponentJpaRepository;
import com.titanium.product.repository.ChargeComponentRepository;

import lombok.RequiredArgsConstructor;

/**
 * 费用项目录关系型仓储适配器。
 */
@Repository
@RequiredArgsConstructor
public class JpaChargeComponentRepository implements ChargeComponentRepository {

    private final ChargeComponentJpaRepository jpaRepository;

    @Override
    public boolean existsByBusinessKey(
            String tenantId, String productId, String componentCode, String componentVersion) {
        return jpaRepository.existsByTenantIdAndProductIdAndComponentCodeAndComponentVersion(
                tenantId, productId, componentCode, componentVersion);
    }

    @Override
    public Optional<ChargeComponentDefinition> findById(
            String tenantId, String productId, String componentId) {
        return jpaRepository.findByComponentIdAndTenantIdAndProductId(componentId, tenantId, productId)
                .map(this::toDomain);
    }

    @Override
    public Optional<ChargeComponentDefinition> findPublished(
            String tenantId,
            String productId,
            String componentCode,
            String componentVersion,
            LocalDateTime businessTime) {
        return jpaRepository.findPublished(
                        tenantId, productId, componentCode, componentVersion, businessTime)
                .map(this::toDomain);
    }

    @Override
    public List<ChargeComponentDefinition> findAll(
            String tenantId, String productId, ActuarialDefinitionStatus status) {
        List<ChargeComponentEntity> entities = status == null
                ? jpaRepository.findByTenantIdAndProductIdOrderByCreateTimeDesc(tenantId, productId)
                : jpaRepository.findByTenantIdAndProductIdAndStatusOrderByCreateTimeDesc(
                        tenantId, productId, status);
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public void save(ChargeComponentDefinition component) {
        jpaRepository.save(toEntity(component));
    }

    private ChargeComponentEntity toEntity(ChargeComponentDefinition component) {
        ChargeComponentEntity entity = new ChargeComponentEntity();
        entity.setComponentId(component.getComponentId());
        entity.setProductId(component.getProductId());
        entity.setComponentCode(component.getComponentCode());
        entity.setComponentVersion(component.getComponentVersion());
        entity.setComponentName(component.getComponentName());
        entity.setDescription(component.getDescription());
        entity.setCategory(component.getCategory());
        entity.setAmountChannel(component.getAmountChannel());
        entity.setDirection(component.getDirection());
        entity.setPayerType(component.getPayerType());
        entity.setCalculationSource(component.getCalculationSource());
        entity.setAccountingClass(component.getAccountingClass());
        entity.setCustomerVisible(component.isCustomerVisible());
        entity.setEffectiveFrom(component.getEffectiveFrom());
        entity.setEffectiveTo(component.getEffectiveTo());
        entity.setTenantId(component.getTenantId());
        entity.setStatus(component.getStatus());
        entity.setContentHash(component.getContentHash());
        return entity;
    }

    private ChargeComponentDefinition toDomain(ChargeComponentEntity entity) {
        return ChargeComponentDefinition.restore(
                entity.getComponentId(), entity.getProductId(), entity.getComponentCode(),
                entity.getComponentVersion(), entity.getComponentName(), entity.getDescription(),
                entity.getCategory(), entity.getAmountChannel(), entity.getDirection(), entity.getPayerType(),
                entity.getCalculationSource(), entity.getAccountingClass(), entity.isCustomerVisible(),
                entity.getEffectiveFrom(), entity.getEffectiveTo(), entity.getTenantId(), entity.getStatus(),
                entity.getContentHash());
    }
}
