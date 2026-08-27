package com.titanium.product.infrastructure.pricing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.pricing.entity.PremiumLifecycleAdjustmentEntity;

/**
 * 生命周期费用差额事实 JPA 仓储。
 */
public interface PremiumLifecycleAdjustmentJpaRepository
        extends JpaRepository<PremiumLifecycleAdjustmentEntity, String> {

    Optional<PremiumLifecycleAdjustmentEntity> findByAdjustmentIdAndTenantId(
            String adjustmentId, String tenantId);

    Optional<PremiumLifecycleAdjustmentEntity> findByTenantIdAndAdjustmentRequestId(
            String tenantId, String adjustmentRequestId);

    Optional<PremiumLifecycleAdjustmentEntity> findByTenantIdAndReversalOfAdjustmentId(
            String tenantId, String reversalOfAdjustmentId);
}
