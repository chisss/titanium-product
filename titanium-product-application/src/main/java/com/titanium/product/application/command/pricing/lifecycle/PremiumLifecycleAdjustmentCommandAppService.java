package com.titanium.product.application.command.pricing.lifecycle;

import org.springframework.stereotype.Service;

import com.titanium.product.application.orchestration.pricing.lifecycle.PremiumLifecycleAdjustmentApplicationService;
import com.titanium.product.command.pricing.lifecycle.CreatePremiumLifecycleAdjustmentCommand;
import com.titanium.product.command.pricing.lifecycle.CreatePremiumLifecycleReversalCommand;
import com.titanium.product.pricing.aggregate.lifecycle.PremiumLifecycleAdjustment;

import lombok.RequiredArgsConstructor;

/**
 * 生命周期费用差额写侧应用入口。
 */
@Service
@RequiredArgsConstructor
public class PremiumLifecycleAdjustmentCommandAppService {

    private final PremiumLifecycleAdjustmentApplicationService applicationService;

    public PremiumLifecycleAdjustment create(CreatePremiumLifecycleAdjustmentCommand command) {
        return applicationService.create(command);
    }

    public PremiumLifecycleAdjustment createReversal(CreatePremiumLifecycleReversalCommand command) {
        return applicationService.createReversal(command);
    }
}
