package com.titanium.product.infrastructure.pricing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.pricing.entity.PremiumLifecycleDifferenceLineEntity;
import com.titanium.product.infrastructure.pricing.entity.PremiumLifecycleDifferenceLineId;

/**
 * 生命周期费用差额行 JPA 仓储。
 */
public interface PremiumLifecycleDifferenceLineJpaRepository
        extends JpaRepository<PremiumLifecycleDifferenceLineEntity, PremiumLifecycleDifferenceLineId> {

    List<PremiumLifecycleDifferenceLineEntity> findByIdAdjustmentIdOrderByIdLineIdAsc(String adjustmentId);
}
