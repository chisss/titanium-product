package com.titanium.product.infrastructure.pricing.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.infrastructure.pricing.entity.ChargeComponentEntity;

/**
 * 费用项目录 JPA 仓储。
 */
public interface ChargeComponentJpaRepository extends JpaRepository<ChargeComponentEntity, String> {

    boolean existsByTenantIdAndProductIdAndComponentCodeAndComponentVersion(
            String tenantId, String productId, String componentCode, String componentVersion);

    Optional<ChargeComponentEntity> findByComponentIdAndTenantIdAndProductId(
            String componentId, String tenantId, String productId);

    List<ChargeComponentEntity> findByTenantIdAndProductIdOrderByCreateTimeDesc(
            String tenantId, String productId);

    List<ChargeComponentEntity> findByTenantIdAndProductIdAndStatusOrderByCreateTimeDesc(
            String tenantId, String productId, ActuarialDefinitionStatus status);

    @Query("""
            select component from ChargeComponentEntity component
             where component.tenantId = :tenantId
               and component.productId = :productId
               and component.componentCode = :componentCode
               and component.componentVersion = :componentVersion
               and component.status = com.titanium.product.common.enums.ActuarialDefinitionStatus.PUBLISHED
               and component.effectiveFrom <= :businessTime
               and (component.effectiveTo is null or :businessTime < component.effectiveTo)
            """)
    Optional<ChargeComponentEntity> findPublished(
            @Param("tenantId") String tenantId,
            @Param("productId") String productId,
            @Param("componentCode") String componentCode,
            @Param("componentVersion") String componentVersion,
            @Param("businessTime") LocalDateTime businessTime);
}
