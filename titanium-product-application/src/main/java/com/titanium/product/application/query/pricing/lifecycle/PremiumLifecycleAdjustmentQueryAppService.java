package com.titanium.product.application.query.pricing.lifecycle;

import org.springframework.stereotype.Service;

import com.titanium.product.aggregate.lifecycle.PremiumLifecycleAdjustment;
import com.titanium.product.application.orchestration.pricing.lifecycle.PremiumLifecycleAdjustmentApplicationService;

import lombok.RequiredArgsConstructor;

/**
 * 生命周期费用差额读侧应用入口。
 */
@Service
@RequiredArgsConstructor
public class PremiumLifecycleAdjustmentQueryAppService {

    private final PremiumLifecycleAdjustmentApplicationService applicationService;

    public PremiumLifecycleAdjustment get(String tenantId, String adjustmentId) {
        return applicationService.get(tenantId, adjustmentId);
    }
}
