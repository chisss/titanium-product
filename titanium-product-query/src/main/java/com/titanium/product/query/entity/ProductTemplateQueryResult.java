package com.titanium.product.query.entity;

import java.util.List;

import com.titanium.product.domain.valueobject.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 产品模板查询结果
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductTemplateQueryResult {

    private String templateId;
    private String templateCode;
    private String templateName;
    private String insuranceCategory;
    private String insuranceType;
    private String productId;
    private String issuanceMode;
    private List<PolicyStage> policyStages;
    private UnderwritingConfig underwritingConfig;
    private PolicyStructureConfig policyStructure;
    private MaintenanceConfig maintenanceConfig;
    private ClaimConfig claimConfig;
    private BillingConfig billingConfig;
    private ReinsuranceConfig reinsuranceConfig;
    private String status;
    private String tenantId;
}
