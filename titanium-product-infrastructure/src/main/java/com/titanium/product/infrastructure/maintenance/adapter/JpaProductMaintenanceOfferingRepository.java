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
import com.titanium.product.infrastructure.maintenance.entity.ProductMaintenanceOfferingEntity;
import com.titanium.product.infrastructure.maintenance.repository.ProductMaintenanceOfferingJpaRepository;
import com.titanium.product.maintenance.aggregate.ProductMaintenanceOffering;
import com.titanium.product.maintenance.repository.ProductMaintenanceOfferingRepository;

import lombok.RequiredArgsConstructor;

/** Product 保全 Offering 仓储基础设施适配器。 */
@Repository
@RequiredArgsConstructor
public class JpaProductMaintenanceOfferingRepository implements ProductMaintenanceOfferingRepository {

    private final ProductMaintenanceOfferingJpaRepository jpaRepository;

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
        List<ProductMaintenanceOfferingEntity> offerings = jpaRepository.findEffectiveOfferings(
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
        jpaRepository.save(toEntity(offering));
    }

    private ProductMaintenanceOffering toDomain(ProductMaintenanceOfferingEntity entity) {
        return ProductMaintenanceOffering.restore(
                entity.getOfferingId(), entity.getTenantId(), entity.getProductId(), entity.getProductVersion(),
                entity.getPlanVersion(), entity.getOfferingVersion(), entity.getEffectiveFrom(),
                entity.getEffectiveTo(), parseCodes(entity.getAllowedPolicyStatusesJson()),
                parseCodes(entity.getAllowedChannelsJson()), parseCodes(entity.getAllowedItemCodesJson()),
                entity.getStatus(), entity.getContentHash());
    }

    private ProductMaintenanceOfferingEntity toEntity(ProductMaintenanceOffering offering) {
        ProductMaintenanceOfferingEntity entity = new ProductMaintenanceOfferingEntity();
        entity.setOfferingId(offering.offeringId());
        entity.setTenantId(offering.tenantId());
        entity.setProductId(offering.productId());
        entity.setProductVersion(offering.productVersion());
        entity.setPlanVersion(offering.planVersion());
        entity.setOfferingVersion(offering.offeringVersion());
        entity.setEffectiveFrom(offering.effectiveFrom());
        entity.setEffectiveTo(offering.effectiveTo());
        entity.setStatus(offering.status());
        entity.setAllowedPolicyStatusesJson(JSON.toJSONString(offering.allowedPolicyStatuses()));
        entity.setAllowedChannelsJson(JSON.toJSONString(offering.allowedChannels()));
        entity.setAllowedItemCodesJson(JSON.toJSONString(offering.allowedItemCodes()));
        entity.setContentHash(offering.contentHash());
        return entity;
    }

    private Set<String> parseCodes(String json) {
        if (json == null || json.isBlank()) {
            return Set.of();
        }
        return new TreeSet<>(JSON.parseArray(json, String.class));
    }
}
