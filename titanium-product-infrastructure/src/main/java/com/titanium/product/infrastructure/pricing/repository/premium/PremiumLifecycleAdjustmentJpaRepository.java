package com.titanium.product.infrastructure.pricing.repository.premium;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.pricing.entity.premium.PremiumLifecycleAdjustmentDO;

/**
 * 生命周期费用差额事实 JPA 仓储。
 */
public interface PremiumLifecycleAdjustmentJpaRepository
        extends JpaRepository<PremiumLifecycleAdjustmentDO, String> {

    Optional<PremiumLifecycleAdjustmentDO> findByAdjustmentIdAndTenantId(
            String adjustmentId, String tenantId);

    Optional<PremiumLifecycleAdjustmentDO> findByTenantIdAndAdjustmentRequestId(
            String tenantId, String adjustmentRequestId);

    Optional<PremiumLifecycleAdjustmentDO> findByTenantIdAndReversalOfAdjustmentId(
            String tenantId, String reversalOfAdjustmentId);
}
