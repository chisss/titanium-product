package com.titanium.product.domain.event;

import java.time.LocalDateTime;
import java.util.List;

import com.titanium.metadata.enums.CommonStatus;
import com.titanium.metadata.enums.InsuranceType;
import com.titanium.product.domain.valueobject.ClaimConfig;
import com.titanium.product.domain.valueobject.IssuanceProcessConfig;
import com.titanium.product.domain.valueobject.MaintenanceConfig;
import com.titanium.product.domain.valueobject.PolicyFormConfig;
import com.titanium.product.domain.valueobject.PricingBasicRule;
import com.titanium.product.domain.valueobject.UnderwritingConfig;

/**
 * 产品模板创建事件
 */
public record ProductTemplateCreatedEvent(
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
        CommonStatus status,
        String tenantId,
        String createdBy,
        LocalDateTime occurredAt
) {
    // 可以添加静态工厂方法
    public static ProductTemplateCreatedEvent of(
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
        return new ProductTemplateCreatedEvent(
                templateId,
                templateCode,
                templateName,
                insuranceType,
                description,
                issuanceProcessConfig,
                underwritingConfig,
                claimsConfig,
                maintenanceConfig,
                policyFormConfig,
                pricingBasicRule,
                supportedCoverages,
                supportedExclusions,
                CommonStatus.ACTIVE,
                tenantId,
                createdBy,
                LocalDateTime.now()
        );
    }
}
