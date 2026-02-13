package com.titanium.product.domain.command;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.product.domain.valueobject.*;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.List;

/**
 * 创建产品模板命令
 */
public record CreateProductTemplateCommand(
        @TargetAggregateIdentifier String templateId,
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
        String tenantId
) {
}
