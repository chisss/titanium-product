package com.titanium.product.infrastructure.pricing.repository.calculation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.pricing.entity.calculation.CalculationNodeDO;

/**
 * 计算节点 JPA 仓储。
 */
public interface CalculationNodeJpaRepository extends JpaRepository<CalculationNodeDO, String> {

    List<CalculationNodeDO> findByModelIdOrderByExecutionOrderAsc(String modelId);

    void deleteByModelId(String modelId);
}
