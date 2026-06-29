package com.titanium.product.infrastructure.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.product.domain.aggregate.ProductTemplate;
import com.titanium.product.domain.repository.ProductTemplateRepository;
import com.titanium.product.domain.valueobject.ClaimConfig;
import com.titanium.product.domain.valueobject.IssuanceProcessConfig;
import com.titanium.product.domain.valueobject.MaintenanceConfig;
import com.titanium.product.domain.valueobject.PolicyFormConfig;
import com.titanium.product.domain.valueobject.UnderwritingConfig;
import com.titanium.product.infrastructure.entity.ProductTemplateEntity;
import com.titanium.product.infrastructure.repository.jpa.ProductTemplateJpaRepository;

import lombok.RequiredArgsConstructor;

/**
 * 产品模板仓储实现
 */
@Component
@RequiredArgsConstructor
public class ProductTemplateRepositoryImpl implements ProductTemplateRepository {

    private final ProductTemplateJpaRepository jpaRepository;

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
                .filter(d -> insuranceType == d.getInsuranceType())
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

    private ProductTemplate toDomain(ProductTemplateEntity entity) {
        IssuanceProcessConfig issuanceProcessConfig = entity.getIssuanceMode() != null
                && entity.getIssuanceMode().startsWith("{")
                        ? JSON.parseObject(entity.getIssuanceMode(), IssuanceProcessConfig.class)
                        : null;
        PolicyFormConfig policyFormConfig = entity.getPolicyStructureJson() != null
                ? JSON.parseObject(entity.getPolicyStructureJson(), PolicyFormConfig.class)
                : null;
        return ProductTemplate.reconstruct(
                entity.getTemplateId(),
                entity.getTemplateCode(),
                entity.getTemplateName(),
                entity.getInsuranceType(),
                issuanceProcessConfig,
                JSON.parseObject(entity.getUnderwritingConfigJson(), UnderwritingConfig.class),
                JSON.parseObject(entity.getClaimConfigJson(), ClaimConfig.class),
                JSON.parseObject(entity.getMaintenanceConfigJson(), MaintenanceConfig.class),
                policyFormConfig,
                entity.getStatus(),
                entity.getTenantId());
    }
}
