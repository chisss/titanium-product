package com.titanium.product.application.command.pricing;

import org.springframework.stereotype.Service;

import com.titanium.product.application.orchestration.pricing.PricingPlanManagementApplicationService;
import com.titanium.product.command.pricing.CreatePricingPlanDraftCommand;
import com.titanium.product.command.pricing.ReplacePricingTestCasesCommand;
import com.titanium.product.valueobject.pricing.PricingPlanValidationResult;

import lombok.RequiredArgsConstructor;

/**
 * 定价方案命令入口。
 */
@Service
@RequiredArgsConstructor
public class PricingPlanCommandAppService {

    private final PricingPlanManagementApplicationService managementService;

    public String createDraft(CreatePricingPlanDraftCommand command) {
        return managementService.createDraft(command);
    }

    public void replaceTestCases(ReplacePricingTestCasesCommand command) {
        managementService.replaceTestCases(command);
    }

    public String approve(String tenantId, String productId, String planId) {
        return managementService.approve(tenantId, productId, planId);
    }

    public PricingPlanValidationResult publish(String tenantId, String productId, String planId) {
        return managementService.publish(tenantId, productId, planId);
    }

    public void retire(String tenantId, String productId, String planId) {
        managementService.retire(tenantId, productId, planId);
    }
}
