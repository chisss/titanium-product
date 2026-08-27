package com.titanium.product.infrastructure.maintenance.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.titanium.product.infrastructure.maintenance.entity.ProductMaintenanceOfferingEntity;

/** Product 保全 Offering JPA Repository。 */
public interface ProductMaintenanceOfferingJpaRepository
        extends JpaRepository<ProductMaintenanceOfferingEntity, String> {

    boolean existsByTenantIdAndProductIdAndProductVersionAndPlanVersionAndOfferingVersion(
            String tenantId,
            String productId,
            String productVersion,
            String planVersion,
            String offeringVersion);

    Optional<ProductMaintenanceOfferingEntity> findByOfferingIdAndTenantIdAndProductId(
            String offeringId, String tenantId, String productId);

    @Query("""
            select offering from ProductMaintenanceOfferingEntity offering
             where offering.tenantId = :tenantId
               and offering.productId = :productId
               and offering.productVersion = :productVersion
               and offering.planVersion = :planVersion
               and offering.status = com.titanium.product.common.enums.ProductMaintenanceOfferingStatus.PUBLISHED
               and offering.effectiveFrom <= :businessTime
               and (offering.effectiveTo is null or :businessTime < offering.effectiveTo)
            """)
    List<ProductMaintenanceOfferingEntity> findEffectiveOfferings(
            @Param("tenantId") String tenantId,
            @Param("productId") String productId,
            @Param("productVersion") String productVersion,
            @Param("planVersion") String planVersion,
            @Param("businessTime") LocalDateTime businessTime);

    @Query("""
            select count(offering) from ProductMaintenanceOfferingEntity offering
             where offering.tenantId = :tenantId
               and offering.productId = :productId
               and offering.productVersion = :productVersion
               and offering.planVersion = :planVersion
               and offering.offeringId <> :excludedOfferingId
               and offering.status = com.titanium.product.common.enums.ProductMaintenanceOfferingStatus.PUBLISHED
               and (:effectiveTo is null or offering.effectiveFrom < :effectiveTo)
               and (offering.effectiveTo is null or offering.effectiveTo > :effectiveFrom)
            """)
    long countPublishedOverlaps(
            @Param("tenantId") String tenantId,
            @Param("productId") String productId,
            @Param("productVersion") String productVersion,
            @Param("planVersion") String planVersion,
            @Param("excludedOfferingId") String excludedOfferingId,
            @Param("effectiveFrom") LocalDateTime effectiveFrom,
            @Param("effectiveTo") LocalDateTime effectiveTo);
}
