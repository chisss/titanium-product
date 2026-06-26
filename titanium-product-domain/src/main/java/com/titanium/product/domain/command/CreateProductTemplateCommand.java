package com.titanium.product.domain.command;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.product.domain.valueobject.*;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.List;

/**
 * 创建产品模板命令
 */
public record CreateProductTemplateCommand(
        @TargetAggregateIdentifier
        String templateId,
        String templateCode,
        String templateName,
        InsuranceType insuranceType,
        String description,
        IssuanceProcessConfig issuanceProcessConfig,
        UnderwritingConfig underwritingConfig,
        ClaimConfig claimsConfig,
        MaintenanceConfig maintenanceConfig,
        PolicyFormConfig policyFormConfig,
        PricingBasicRule pricingBasicRule,
        List<String> supportedCoverages,
        List<String> supportedExclusions,
        String tenantId,
        String createdBy
) {
}
