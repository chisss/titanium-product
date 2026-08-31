package com.titanium.product.infrastructure.pricing.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.titanium.product.aggregate.ChargeComponentDefinition;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.infrastructure.mapper.ChargeComponentPersistenceMapper;
import com.titanium.product.infrastructure.pricing.entity.ChargeComponentDO;
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
    private final ChargeComponentPersistenceMapper persistenceMapper;

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
        List<ChargeComponentDO> dataObjects = status == null
                ? jpaRepository.findByTenantIdAndProductIdOrderByCreateTimeDesc(tenantId, productId)
                : jpaRepository.findByTenantIdAndProductIdAndStatusOrderByCreateTimeDesc(
                        tenantId, productId, status);
        return dataObjects.stream().map(this::toDomain).toList();
    }

    @Override
    public void save(ChargeComponentDefinition component) {
        jpaRepository.save(persistenceMapper.toDO(component));
    }

    private ChargeComponentDefinition toDomain(ChargeComponentDO dataObject) {
        return ChargeComponentDefinition.restore(
                dataObject.getComponentId(), dataObject.getProductId(), dataObject.getComponentCode(),
                dataObject.getComponentVersion(), dataObject.getComponentName(), dataObject.getDescription(),
                dataObject.getCategory(), dataObject.getAmountChannel(), dataObject.getDirection(),
                dataObject.getPayerType(), dataObject.getCalculationSource(), dataObject.getAccountingClass(),
                dataObject.isCustomerVisible(), dataObject.getEffectiveFrom(), dataObject.getEffectiveTo(),
                dataObject.getTenantId(), dataObject.getStatus(), dataObject.getContentHash());
    }
}
