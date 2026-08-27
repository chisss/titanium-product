package com.titanium.product.infrastructure.pricing.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.pricing.entity.CalculationTotalEntity;

/**
 * Product 确认计算费用汇总 JPA 仓储。
 */
public interface CalculationTotalJpaRepository extends JpaRepository<CalculationTotalEntity, String> {
}
