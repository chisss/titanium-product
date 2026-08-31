package com.titanium.product.infrastructure.pricing.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.infrastructure.pricing.entity.CalculationModelDO;

/**
 * 结构化计算模型 JPA 仓储。
 */
public interface CalculationModelJpaRepository extends JpaRepository<CalculationModelDO, String> {

    boolean existsByTenantIdAndProductIdAndModelCodeAndModelVersion(
            String tenantId, String productId, String modelCode, String modelVersion);

    Optional<CalculationModelDO> findByModelIdAndTenantIdAndProductId(
            String modelId, String tenantId, String productId);

    List<CalculationModelDO> findByTenantIdAndProductIdOrderByCreateTimeDesc(
            String tenantId, String productId);

    List<CalculationModelDO> findByTenantIdAndProductIdAndStatusOrderByCreateTimeDesc(
            String tenantId, String productId, ActuarialDefinitionStatus status);

    @Query("""
            select model from CalculationModelDO model
             where model.tenantId = :tenantId
               and model.productId = :productId
               and model.modelCode = :modelCode
               and model.modelVersion = :modelVersion
               and model.status = com.titanium.product.common.enums.ActuarialDefinitionStatus.PUBLISHED
               and model.effectiveFrom <= :businessTime
               and (model.effectiveTo is null or :businessTime < model.effectiveTo)
            """)
    Optional<CalculationModelDO> findPublished(
            @Param("tenantId") String tenantId,
            @Param("productId") String productId,
            @Param("modelCode") String modelCode,
            @Param("modelVersion") String modelVersion,
            @Param("businessTime") LocalDateTime businessTime);
}
