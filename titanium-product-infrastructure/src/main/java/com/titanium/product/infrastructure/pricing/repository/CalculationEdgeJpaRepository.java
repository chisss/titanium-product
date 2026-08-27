package com.titanium.product.infrastructure.pricing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.pricing.entity.CalculationEdgeEntity;

/**
 * 计算依赖边 JPA 仓储。
 */
public interface CalculationEdgeJpaRepository extends JpaRepository<CalculationEdgeEntity, String> {

    List<CalculationEdgeEntity> findByModelIdOrderByFromNodeCodeAscToNodeCodeAsc(String modelId);

    void deleteByModelId(String modelId);
}
