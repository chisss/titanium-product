package com.titanium.product.infrastructure.pricing.repository.premium;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.pricing.entity.premium.PremiumLifecycleDifferenceLineDO;
import com.titanium.product.infrastructure.pricing.entity.premium.PremiumLifecycleDifferenceLineId;

/**
 * 生命周期费用差额行 JPA 仓储。
 */
public interface PremiumLifecycleDifferenceLineJpaRepository
        extends JpaRepository<PremiumLifecycleDifferenceLineDO, PremiumLifecycleDifferenceLineId> {

    List<PremiumLifecycleDifferenceLineDO> findByIdAdjustmentIdOrderByIdLineIdAsc(String adjustmentId);
}
