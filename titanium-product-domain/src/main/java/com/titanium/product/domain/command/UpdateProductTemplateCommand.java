package com.titanium.product.domain.command;

import java.util.List;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.domain.valueobject.BillingConfig;
import com.titanium.product.domain.valueobject.ClaimConfig;
import com.titanium.product.domain.valueobject.MaintenanceConfig;
import com.titanium.product.domain.valueobject.PolicyStage;
import com.titanium.product.domain.valueobject.PolicyStructureConfig;
import com.titanium.product.domain.valueobject.ReinsuranceConfig;
import com.titanium.product.domain.valueobject.UnderwritingConfig;

/**
 * 更新产品模板命令
 */
public record UpdateProductTemplateCommand(
        @TargetAggregateIdentifier String templateId,
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
