package com.titanium.product.infrastructure.pricing.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.infrastructure.pricing.entity.DynamicFactorEntity;

public interface DynamicFactorJpaRepository extends JpaRepository<DynamicFactorEntity, String> {

    boolean existsByTenantIdAndProductIdAndFactorCodeAndFactorVersion(
            String tenantId, String productId, String factorCode, String factorVersion);

    Optional<DynamicFactorEntity> findByFactorIdAndTenantIdAndProductId(
            String factorId, String tenantId, String productId);

    List<DynamicFactorEntity> findByTenantIdAndProductIdOrderByFactorCodeAscFactorVersionDesc(
            String tenantId, String productId);

    List<DynamicFactorEntity> findByTenantIdAndProductIdAndStatusOrderByFactorCodeAscFactorVersionDesc(
            String tenantId, String productId, ActuarialDefinitionStatus status);

    @Query("""
            select f from DynamicFactorEntity f
            where f.tenantId = :tenantId and f.productId = :productId
              and f.factorCode = :factorCode and f.factorVersion = :factorVersion
              and f.status = com.titanium.product.common.enums.ActuarialDefinitionStatus.PUBLISHED
              and f.effectiveFrom <= :businessTime
              and (f.effectiveTo is null or f.effectiveTo > :businessTime)
            """)
    Optional<DynamicFactorEntity> findPublished(
            @Param("tenantId") String tenantId,
            @Param("productId") String productId,
            @Param("factorCode") String factorCode,
            @Param("factorVersion") String factorVersion,
            @Param("businessTime") LocalDateTime businessTime);
}
