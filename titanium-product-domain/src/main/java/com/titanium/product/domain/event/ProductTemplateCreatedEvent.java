package com.titanium.product.domain.event;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.product.domain.valueobject.*;

import java.util.List;

/**
 * 产品模板创建事件
 */
public record ProductTemplateCreatedEvent(
        String templateId,
        String templateCode,
        String templateName,
        String insuranceCategory,
        InsuranceType insuranceType,
        String productId,
        IssuanceMode issuanceMode,
        List<PolicyStage> policyStages,
        UnderwritingConfig underwritingConfig,
        PolicyStructureConfig policyStructure,
        MaintenanceConfig maintenanceConfig,
        ClaimConfig claimConfig,
        BillingConfig billingConfig,
        ReinsuranceConfig reinsuranceConfig,
        String status,
        String tenantId
) {
}
