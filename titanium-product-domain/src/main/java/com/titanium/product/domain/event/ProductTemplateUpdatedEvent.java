package com.titanium.product.domain.event;

import com.titanium.product.domain.valueobject.*;

import java.util.List;

/**
 * 产品模板更新事件
 */
public record ProductTemplateUpdatedEvent(
        String templateId,
        String templateName,
        IssuanceMode issuanceMode,
        List<PolicyStage> policyStages,
        UnderwritingConfig underwritingConfig,
        PolicyStructureConfig policyStructure,
        MaintenanceConfig maintenanceConfig,
        ClaimConfig claimConfig,
        BillingConfig billingConfig,
        ReinsuranceConfig reinsuranceConfig,
        String tenantId
) {
}
