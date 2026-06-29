package com.titanium.product.query.handler;


import java.util.List;

import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import com.titanium.product.domain.query.GetTemplateByCodeQuery;
import com.titanium.product.domain.query.GetTemplateByIdQuery;
import com.titanium.product.domain.query.GetTemplateByProductIdQuery;
import com.titanium.product.domain.query.GetTemplatesByInsuranceTypeQuery;
import com.titanium.product.domain.valueobject.BillingConfig;
import com.titanium.product.domain.valueobject.ClaimConfig;
import com.titanium.product.domain.valueobject.MaintenanceConfig;
import com.titanium.product.domain.valueobject.PolicyStage;
import com.titanium.product.domain.valueobject.PolicyStructureConfig;
import com.titanium.product.domain.valueobject.ReinsuranceConfig;
import com.titanium.product.domain.valueobject.UnderwritingConfig;
import com.titanium.product.infrastructure.entity.ProductTemplateEntity;
import com.titanium.product.infrastructure.repository.jpa.ProductTemplateJpaRepository;
import com.titanium.product.query.entity.ProductTemplateQueryResult;

import lombok.RequiredArgsConstructor;

/**
 * 产品模板查询处理器
 */
@Component
@RequiredArgsConstructor
public class ProductTemplateQueryHandler {

    private final ProductTemplateJpaRepository jpaRepository;

    @QueryHandler
    public ProductTemplateQueryResult handle(GetTemplateByIdQuery query) {
        return jpaRepository.findById(query.templateId())
                .map(this::toQueryResult)
                .orElse(null);
    }

    @QueryHandler
    public ProductTemplateQueryResult handle(GetTemplateByProductIdQuery query) {
        return jpaRepository.findByProductIdAndTenantId(query.productId(), query.tenantId())
                .map(this::toQueryResult)
                .orElse(null);
    }

    @QueryHandler
    public ProductTemplateQueryResult handle(GetTemplateByCodeQuery query) {
        return jpaRepository.findByTemplateCodeAndTenantId(query.templateCode(), query.tenantId())
                .map(this::toQueryResult)
                .orElse(null);
    }

    @QueryHandler
    public List<ProductTemplateQueryResult> handle(GetTemplatesByInsuranceTypeQuery query) {
        return jpaRepository.findByInsuranceTypeAndTenantId(query.insuranceType(), query.tenantId())
                .stream()
                .map(this::toQueryResult)
                .toList();
    }

    private ProductTemplateQueryResult toQueryResult(ProductTemplateEntity entity) {
        return ProductTemplateQueryResult.builder()
                .templateId(entity.getTemplateId())
                .templateCode(entity.getTemplateCode())
                .templateName(entity.getTemplateName())
                .insuranceCategory(entity.getInsuranceCategory())
                .insuranceType(entity.getInsuranceType())
                .productId(entity.getProductId())
                .issuanceMode(entity.getIssuanceMode())
                .policyStages(JSON.parseObject(entity.getPolicyStagesJson(),
                        new TypeReference<List<PolicyStage>>() {}))
                .underwritingConfig(JSON.parseObject(entity.getUnderwritingConfigJson(),
                        UnderwritingConfig.class))
                .policyStructure(JSON.parseObject(entity.getPolicyStructureJson(),
                        PolicyStructureConfig.class))
                .maintenanceConfig(JSON.parseObject(entity.getMaintenanceConfigJson(),
                        MaintenanceConfig.class))
                .claimConfig(JSON.parseObject(entity.getClaimConfigJson(),
                        ClaimConfig.class))
                .billingConfig(JSON.parseObject(entity.getBillingConfigJson(),
                        BillingConfig.class))
                .reinsuranceConfig(entity.getReinsuranceConfigJson() != null
                        ? JSON.parseObject(entity.getReinsuranceConfigJson(), ReinsuranceConfig.class)
                        : null)
                .status(entity.getStatus())
                .tenantId(entity.getTenantId())
                .build();
    }
}
