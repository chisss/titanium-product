package com.titanium.product.application.query.pricing;

import java.util.List;

import org.springframework.stereotype.Service;

import com.titanium.product.aggregate.PricingPlanDefinition;
import com.titanium.product.application.orchestration.pricing.PricingPlanManagementApplicationService;
import com.titanium.product.common.enums.PricingPlanStatus;
import com.titanium.product.valueobject.pricing.PricingPlanValidationResult;

import lombok.RequiredArgsConstructor;

/**
 * 定价方案查询入口。
 */
@Service
@RequiredArgsConstructor
public class PricingPlanQueryAppService {

    private final PricingPlanManagementApplicationService managementService;

    public PricingPlanDefinition get(String tenantId, String productId, String planId) {
        return managementService.get(tenantId, productId, planId);
    }

    public List<PricingPlanDefinition> list(
            String tenantId, String productId, PricingPlanStatus status) {
        return managementService.list(tenantId, productId, status);
    }

    public PricingPlanValidationResult runTests(String tenantId, String productId, String planId) {
        return managementService.runTests(tenantId, productId, planId);
    }
}
