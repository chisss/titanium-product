package com.titanium.product.query.entity;

import java.util.List;

import com.titanium.metadata.enums.CommonStatus;
import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.domain.valueobject.BillingConfig;
import com.titanium.product.domain.valueobject.ClaimConfig;
import com.titanium.product.domain.valueobject.MaintenanceConfig;
import com.titanium.product.domain.valueobject.PolicyStage;
import com.titanium.product.domain.valueobject.PolicyStructureConfig;
import com.titanium.product.domain.valueobject.ReinsuranceConfig;
import com.titanium.product.domain.valueobject.UnderwritingConfig;

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
    private ProductEnum.ProductCategory insuranceCategory;
    private InsuranceType insuranceType;
    private String productId;
    private String issuanceMode;
    private List<PolicyStage> policyStages;
    private UnderwritingConfig underwritingConfig;
    private PolicyStructureConfig policyStructure;
    private MaintenanceConfig maintenanceConfig;
    private ClaimConfig claimConfig;
    private BillingConfig billingConfig;
    private ReinsuranceConfig reinsuranceConfig;
    private CommonStatus status;
    private String tenantId;
}
