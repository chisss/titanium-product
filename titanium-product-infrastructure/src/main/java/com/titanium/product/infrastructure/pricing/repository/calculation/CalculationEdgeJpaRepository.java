package com.titanium.product.infrastructure.pricing.repository.calculation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.pricing.entity.calculation.CalculationEdgeDO;

/**
 * 计算依赖边 JPA 仓储。
 */
public interface CalculationEdgeJpaRepository extends JpaRepository<CalculationEdgeDO, String> {

    List<CalculationEdgeDO> findByModelIdOrderByFromNodeCodeAscToNodeCodeAsc(String modelId);

    void deleteByModelId(String modelId);
}
