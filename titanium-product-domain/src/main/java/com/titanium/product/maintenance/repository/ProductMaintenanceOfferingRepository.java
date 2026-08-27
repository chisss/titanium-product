package com.titanium.product.maintenance.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import com.titanium.product.maintenance.aggregate.ProductMaintenanceOffering;

/** Product 保全 Offering 仓储端口。 */
public interface ProductMaintenanceOfferingRepository {

    boolean existsByBusinessKey(
            String tenantId,
            String productId,
            String productVersion,
            String planVersion,
            String offeringVersion);

    Optional<ProductMaintenanceOffering> findById(
            String tenantId, String productId, String offeringId);

    Optional<ProductMaintenanceOffering> findEffective(
            String tenantId,
            String productId,
            String productVersion,
            String planVersion,
            LocalDateTime businessTime);

    boolean existsPublishedOverlap(
            String tenantId,
            String productId,
            String productVersion,
            String planVersion,
            String excludedOfferingId,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo);

    void save(ProductMaintenanceOffering offering);
}
