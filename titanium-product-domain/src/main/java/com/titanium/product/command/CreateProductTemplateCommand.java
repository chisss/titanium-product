package com.titanium.product.command;

import java.util.List;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.product.valueobject.ClaimConfig;
import com.titanium.product.valueobject.IssuanceProcessConfig;
import com.titanium.product.valueobject.MaintenanceConfig;
import com.titanium.product.valueobject.PolicyFormConfig;
import com.titanium.product.valueobject.PricingBasicRule;
import com.titanium.product.valueobject.UnderwritingConfig;

/**
 * 创建产品模板命令
 */
public record CreateProductTemplateCommand(@TargetAggregateIdentifier String templateId, String templateCode,
                                           String templateName, InsuranceType insuranceType, String description,
                                           IssuanceProcessConfig issuanceProcessConfig,
                                           UnderwritingConfig underwritingConfig, ClaimConfig claimsConfig,
                                           MaintenanceConfig maintenanceConfig, PolicyFormConfig policyFormConfig,
                                           PricingBasicRule pricingBasicRule, List<String> supportedCoverages,
                                           List<String> supportedExclusions, String tenantId, String createdBy) {
}
