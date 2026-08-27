package com.titanium.product.repository;

import java.util.Optional;

import com.titanium.product.aggregate.lifecycle.PremiumLifecycleAdjustment;

/**
 * 生命周期费用差额事实仓储端口。
 */
public interface PremiumLifecycleAdjustmentRepository {

    Optional<PremiumLifecycleAdjustment> findById(String tenantId, String adjustmentId);

    Optional<PremiumLifecycleAdjustment> findByRequestId(String tenantId, String adjustmentRequestId);

    Optional<PremiumLifecycleAdjustment> findByReversalOfAdjustmentId(
            String tenantId, String sourceAdjustmentId);

    void save(PremiumLifecycleAdjustment adjustment);
}
