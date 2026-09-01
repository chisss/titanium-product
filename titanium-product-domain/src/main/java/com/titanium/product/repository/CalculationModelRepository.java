package com.titanium.product.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.pricing.aggregate.CalculationModelDefinition;

/**
 * 结构化计算模型仓储端口。
 */
public interface CalculationModelRepository {

    boolean existsByBusinessKey(String tenantId, String productId, String modelCode, String modelVersion);

    Optional<CalculationModelDefinition> findById(String tenantId, String productId, String modelId);

    Optional<CalculationModelDefinition> findPublished(
            String tenantId,
            String productId,
            String modelCode,
            String modelVersion,
            LocalDateTime businessTime);

    List<CalculationModelDefinition> findAll(
            String tenantId, String productId, ActuarialDefinitionStatus status);

    void save(CalculationModelDefinition model);
}
