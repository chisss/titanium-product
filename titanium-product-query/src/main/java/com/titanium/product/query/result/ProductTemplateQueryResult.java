package com.titanium.product.query.result;

import java.util.List;

import com.titanium.metadata.enums.CommonStatus;
import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.valueobject.LifeProductSpec;
import com.titanium.product.valueobject.PolicyStage;
import com.titanium.product.valueobject.config.BillingConfig;
import com.titanium.product.valueobject.config.ClaimConfig;
import com.titanium.product.valueobject.config.DividendConfig;
import com.titanium.product.valueobject.config.MaintenanceConfig;
import com.titanium.product.valueobject.config.PolicyStructureConfig;
import com.titanium.product.valueobject.config.ReinsuranceConfig;
import com.titanium.product.valueobject.config.UnderwritingConfig;

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
    private DividendConfig dividendConfig;
    /** 寿险产品规格（寿险模板专属：投保年龄/保额范围/缴费期/保障期选项） */
    private LifeProductSpec lifeProductSpec;
    private CommonStatus status;
    private String tenantId;
}
