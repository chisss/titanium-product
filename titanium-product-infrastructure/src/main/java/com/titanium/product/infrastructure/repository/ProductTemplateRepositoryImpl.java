package com.titanium.product.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.titanium.metadata.enums.InsuranceType;
import com.titanium.product.domain.aggregate.ProductTemplate;
import com.titanium.product.domain.repository.ProductTemplateRepository;
import com.titanium.product.domain.valueobject.*;
import com.titanium.product.infrastructure.entity.ProductTemplateDO;
import com.titanium.product.infrastructure.repository.jpa.ProductTemplateJpaRepository;

/**
 * 产品模板仓储实现
 */
@Component
public class ProductTemplateRepositoryImpl implements ProductTemplateRepository {

    @Autowired
    private ProductTemplateJpaRepository jpaRepository;

    @Override
    public Optional<ProductTemplate> findById(String templateId) {
        return jpaRepository.findById(templateId).map(this::toDomain);
    }

    @Override
    public Optional<ProductTemplate> findByProductId(String productId) {
        // 跨租户查询，使用第一个匹配结果
        return jpaRepository.findAll().stream()
                .filter(d -> productId.equals(d.getProductId()))
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    public Optional<ProductTemplate> findByTemplateCode(String templateCode) {
        return jpaRepository.findAll().stream()
                .filter(d -> templateCode.equals(d.getTemplateCode()))
                .findFirst()
                .map(this::toDomain);
    }

    @Override
    public List<ProductTemplate> findByInsuranceType(InsuranceType insuranceType) {
        return jpaRepository.findAll().stream()
                .filter(d -> insuranceType.getCode().equals(d.getInsuranceType()))
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<ProductTemplate> findByTenantId(String tenantId) {
        return jpaRepository.findByTenantId(tenantId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsByTemplateCode(String templateCode) {
        return jpaRepository.existsByTemplateCode(templateCode);
    }

    private ProductTemplate toDomain(ProductTemplateDO entity) {
        return ProductTemplate.builder()
                .templateId(entity.getTemplateId())
                .templateCode(entity.getTemplateCode())
                .templateName(entity.getTemplateName())
                .insuranceCategory(entity.getInsuranceCategory())
                .insuranceType(InsuranceType.fromCode(entity.getInsuranceType()))
                .productId(entity.getProductId())
                .issuanceMode(IssuanceMode.fromCode(entity.getIssuanceMode()))
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
