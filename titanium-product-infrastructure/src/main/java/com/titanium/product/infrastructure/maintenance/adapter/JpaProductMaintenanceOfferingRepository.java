package com.titanium.product.infrastructure.maintenance.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;

import com.titanium.product.common.enums.ProductMaintenanceOfferingFailureReason;
import com.titanium.product.exception.ProductMaintenanceOfferingException;
import com.titanium.product.infrastructure.maintenance.entity.ProductMaintenanceOfferingDO;
import com.titanium.product.infrastructure.maintenance.repository.ProductMaintenanceOfferingJpaRepository;
import com.titanium.product.infrastructure.mapper.ProductMaintenanceOfferingPersistenceMapper;
import com.titanium.product.maintenance.aggregate.ProductMaintenanceOffering;
import com.titanium.product.maintenance.repository.ProductMaintenanceOfferingRepository;

import lombok.RequiredArgsConstructor;

/** Product 保全 Offering 仓储基础设施适配器。 */
@Repository
@RequiredArgsConstructor
public class JpaProductMaintenanceOfferingRepository implements ProductMaintenanceOfferingRepository {

    private final ProductMaintenanceOfferingJpaRepository jpaRepository;
    private final ProductMaintenanceOfferingPersistenceMapper persistenceMapper;

    @Override
    @Transactional(readOnly = true)
    public boolean existsByBusinessKey(
            String tenantId,
            String productId,
            String productVersion,
            String planVersion,
            String offeringVersion) {
        return jpaRepository.existsByTenantIdAndProductIdAndProductVersionAndPlanVersionAndOfferingVersion(
                tenantId, productId, productVersion, planVersion, offeringVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductMaintenanceOffering> findById(
            String tenantId, String productId, String offeringId) {
        return jpaRepository.findByOfferingIdAndTenantIdAndProductId(offeringId, tenantId, productId)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductMaintenanceOffering> findEffective(
            String tenantId,
            String productId,
            String productVersion,
            String planVersion,
            LocalDateTime businessTime) {
        List<ProductMaintenanceOfferingDO> offerings = jpaRepository.findEffectiveOfferings(
                tenantId, productId, productVersion, planVersion, businessTime);
        if (offerings.size() > 1) {
            throw new ProductMaintenanceOfferingException(
                    ProductMaintenanceOfferingFailureReason.CONTRACT_INVALID,
                    "同一产品、计划和业务时点存在多个已发布Offering");
        }
        return offerings.stream().findFirst().map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsPublishedOverlap(
            String tenantId,
            String productId,
            String productVersion,
            String planVersion,
            String excludedOfferingId,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo) {
        return jpaRepository.countPublishedOverlaps(
                tenantId, productId, productVersion, planVersion, excludedOfferingId,
                effectiveFrom, effectiveTo) > 0;
    }

    @Override
    @Transactional
    public void save(ProductMaintenanceOffering offering) {
        jpaRepository.save(persistenceMapper.toDO(offering));
    }

    private ProductMaintenanceOffering toDomain(ProductMaintenanceOfferingDO dataObject) {
        return ProductMaintenanceOffering.restore(
                dataObject.getOfferingId(), dataObject.getTenantId(), dataObject.getProductId(),
                dataObject.getProductVersion(), dataObject.getPlanVersion(), dataObject.getOfferingVersion(),
                dataObject.getEffectiveFrom(), dataObject.getEffectiveTo(),
                parseCodes(dataObject.getAllowedPolicyStatusesJson()),
                parseCodes(dataObject.getAllowedChannelsJson()), parseCodes(dataObject.getAllowedItemCodesJson()),
                dataObject.getStatus(), dataObject.getContentHash());
    }

    private Set<String> parseCodes(String json) {
        if (json == null || json.isBlank()) {
            return Set.of();
        }
        return new TreeSet<>(JSON.parseArray(json, String.class));
    }
}
