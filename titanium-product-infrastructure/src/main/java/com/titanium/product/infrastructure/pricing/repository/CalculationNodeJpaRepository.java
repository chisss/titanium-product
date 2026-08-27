package com.titanium.product.infrastructure.pricing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.pricing.entity.CalculationNodeEntity;

/**
 * 计算节点 JPA 仓储。
 */
public interface CalculationNodeJpaRepository extends JpaRepository<CalculationNodeEntity, String> {

    List<CalculationNodeEntity> findByModelIdOrderByExecutionOrderAsc(String modelId);

    void deleteByModelId(String modelId);
}
