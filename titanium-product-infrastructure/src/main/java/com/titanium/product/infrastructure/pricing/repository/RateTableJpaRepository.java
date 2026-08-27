package com.titanium.product.infrastructure.pricing.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.titanium.product.common.enums.RateTableStatus;
import com.titanium.product.infrastructure.pricing.entity.RateTableEntity;

/**
 * 费率表元数据 JPA 仓储。
 */
public interface RateTableJpaRepository extends JpaRepository<RateTableEntity, String> {

    boolean existsByTenantIdAndProductIdAndTableCodeAndTableVersion(
            String tenantId, String productId, String tableCode, String tableVersion);

    Optional<RateTableEntity> findByTableIdAndTenantIdAndProductId(
            String tableId, String tenantId, String productId);

    List<RateTableEntity> findByTenantIdAndProductIdOrderByCreateTimeDesc(String tenantId, String productId);

    List<RateTableEntity> findByTenantIdAndProductIdAndStatusOrderByCreateTimeDesc(
            String tenantId, String productId, RateTableStatus status);

    @Query("""
            select tableMeta from RateTableEntity tableMeta
             where tableMeta.tenantId = :tenantId
               and tableMeta.productId = :productId
               and tableMeta.tableCode = :tableCode
               and tableMeta.tableVersion = :tableVersion
               and tableMeta.status = com.titanium.product.common.enums.RateTableStatus.PUBLISHED
               and tableMeta.effectiveFrom <= :businessTime
               and (tableMeta.effectiveTo is null or :businessTime < tableMeta.effectiveTo)
            """)
    Optional<RateTableEntity> findEffectiveTable(
            @Param("tenantId") String tenantId,
            @Param("productId") String productId,
            @Param("tableCode") String tableCode,
            @Param("tableVersion") String tableVersion,
            @Param("businessTime") LocalDateTime businessTime);
}
