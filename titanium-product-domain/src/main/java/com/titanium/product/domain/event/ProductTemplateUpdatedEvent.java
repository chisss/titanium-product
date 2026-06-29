package com.titanium.product.domain.event;

import java.util.List;

import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.domain.valueobject.BillingConfig;
import com.titanium.product.domain.valueobject.ClaimConfig;
import com.titanium.product.domain.valueobject.MaintenanceConfig;
import com.titanium.product.domain.valueobject.PolicyStage;
import com.titanium.product.domain.valueobject.PolicyStructureConfig;
import com.titanium.product.domain.valueobject.ReinsuranceConfig;
import com.titanium.product.domain.valueobject.UnderwritingConfig;

/**
 * 产品模板更新事件
 */
public record ProductTemplateUpdatedEvent(
        String templateId,
        String templateName,
        ProductEnum.IssuanceMode issuanceMode,
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
