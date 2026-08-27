package com.titanium.product.infrastructure.pricing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.pricing.entity.CalculationLineEntity;
import com.titanium.product.infrastructure.pricing.entity.CalculationLineId;

/**
 * Product 确认计算费用明细 JPA 仓储。
 */
public interface CalculationLineJpaRepository extends JpaRepository<CalculationLineEntity, CalculationLineId> {

    List<CalculationLineEntity> findByIdCalculationIdOrderByIdLineIdAsc(String calculationId);
}
