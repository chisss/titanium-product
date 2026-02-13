package com.titanium.product.domain.command;

import com.titanium.product.domain.valueobject.*;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.List;

/**
 * 更新产品模板命令
 */
public record UpdateProductTemplateCommand(
        @TargetAggregateIdentifier String templateId,
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
