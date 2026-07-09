package com.titanium.product.infrastructure.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.aggregate.ProductTemplate;
import com.titanium.product.infrastructure.entity.ProductTemplateEntity;
import com.titanium.product.infrastructure.repository.jpa.ProductTemplateJpaRepository;
import com.titanium.product.repository.ProductTemplateRepository;
import com.titanium.product.valueobject.BillingConfig;
import com.titanium.product.valueobject.ClaimConfig;
import com.titanium.product.valueobject.DividendConfig;
import com.titanium.product.valueobject.IssuanceProcessConfig;
import com.titanium.product.valueobject.MaintenanceConfig;
import com.titanium.product.valueobject.PolicyFormConfig;
import com.titanium.product.valueobject.PolicyStage;
import com.titanium.product.valueobject.PolicyStructureConfig;
import com.titanium.product.valueobject.ReinsuranceConfig;
import com.titanium.product.valueobject.UnderwritingConfig;

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
        // 行为配置字段恢复：出单模式为字符串枚举，其余为 JSON 值对象
        ProductEnum.IssuanceMode issuanceMode = resolveIssuanceMode(entity.getIssuanceMode());
        List<PolicyStage> policyStages = entity.getPolicyStagesJson() != null
                ? JSON.parseArray(entity.getPolicyStagesJson(), PolicyStage.class)
                : null;
        PolicyStructureConfig policyStructureConfig = entity.getPolicyStructureJson() != null
                ? JSON.parseObject(entity.getPolicyStructureJson(), PolicyStructureConfig.class)
                : null;
        BillingConfig billingConfig = entity.getBillingConfigJson() != null
                ? JSON.parseObject(entity.getBillingConfigJson(), BillingConfig.class)
                : null;
        ReinsuranceConfig reinsuranceConfig = entity.getReinsuranceConfigJson() != null
                ? JSON.parseObject(entity.getReinsuranceConfigJson(), ReinsuranceConfig.class)
                : null;
        DividendConfig dividendConfig = entity.getDividendConfigJson() != null
                ? JSON.parseObject(entity.getDividendConfigJson(), DividendConfig.class)
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
                issuanceMode,
                policyStages,
                policyStructureConfig,
                billingConfig,
                reinsuranceConfig,
                dividendConfig,
                entity.getStatus(),
                entity.getTenantId());
    }

    /**
     * 解析出单模式：仅当值为合法枚举名时返回，JSON 或空值返回 null（兼容旧数据）。
     */
    private ProductEnum.IssuanceMode resolveIssuanceMode(String rawIssuanceMode) {
        if (rawIssuanceMode == null || rawIssuanceMode.startsWith("{")) {
            return null;
        }
        try {
            return ProductEnum.IssuanceMode.valueOf(rawIssuanceMode);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
