package com.titanium.product.infrastructure.pricing.repository.pricing;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.infrastructure.pricing.entity.pricing.DynamicFactorDO;

public interface DynamicFactorJpaRepository extends JpaRepository<DynamicFactorDO, String> {

    boolean existsByTenantIdAndProductIdAndFactorCodeAndFactorVersion(
            String tenantId, String productId, String factorCode, String factorVersion);

    Optional<DynamicFactorDO> findByFactorIdAndTenantIdAndProductId(
            String factorId, String tenantId, String productId);

    List<DynamicFactorDO> findByTenantIdAndProductIdOrderByFactorCodeAscFactorVersionDesc(
            String tenantId, String productId);

    List<DynamicFactorDO> findByTenantIdAndProductIdAndStatusOrderByFactorCodeAscFactorVersionDesc(
            String tenantId, String productId, ActuarialDefinitionStatus status);

    @Query("""
            select f from DynamicFactorDO f
            where f.tenantId = :tenantId and f.productId = :productId
              and f.factorCode = :factorCode and f.factorVersion = :factorVersion
              and f.status = com.titanium.product.common.enums.ActuarialDefinitionStatus.PUBLISHED
              and f.effectiveFrom <= :businessTime
              and (f.effectiveTo is null or f.effectiveTo > :businessTime)
            """)
    Optional<DynamicFactorDO> findPublished(
            @Param("tenantId") String tenantId,
            @Param("productId") String productId,
            @Param("factorCode") String factorCode,
            @Param("factorVersion") String factorVersion,
            @Param("businessTime") LocalDateTime businessTime);
}
