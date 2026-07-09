package com.titanium.product.event;

import java.time.LocalDateTime;
import java.util.List;

import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.valueobject.BillingConfig;
import com.titanium.product.valueobject.ClaimConfig;
import com.titanium.product.valueobject.DividendConfig;
import com.titanium.product.valueobject.MaintenanceConfig;
import com.titanium.product.valueobject.PolicyStage;
import com.titanium.product.valueobject.PolicyStructureConfig;
import com.titanium.product.valueobject.ReinsuranceConfig;
import com.titanium.product.valueobject.UnderwritingConfig;

/**
 * 产品模板更新事件
 */
public record ProductTemplateUpdatedEvent(String templateId, String templateName, ProductEnum.IssuanceMode issuanceMode,
                                          List<PolicyStage> policyStages, UnderwritingConfig underwritingConfig,
                                          PolicyStructureConfig policyStructure, MaintenanceConfig maintenanceConfig,
                                          ClaimConfig claimConfig, BillingConfig billingConfig,
                                          ReinsuranceConfig reinsuranceConfig, DividendConfig dividendConfig,
                                          String tenantId, LocalDateTime occurredAt) {
}
