package com.titanium.product.query.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.product.query.repository.ProductTemplateViewRepository;
import com.titanium.product.query.result.ProductTemplateQueryResult;
import com.titanium.product.query.service.ProductTemplateQueryService;
import com.titanium.product.query.view.ProductTemplateView;
import com.titanium.product.valueobject.BillingConfig;
import com.titanium.product.valueobject.ClaimConfig;
import com.titanium.product.valueobject.DividendConfig;
import com.titanium.product.valueobject.LifeProductSpec;
import com.titanium.product.valueobject.MaintenanceConfig;
import com.titanium.product.valueobject.PolicyStage;
import com.titanium.product.valueobject.PolicyStructureConfig;
import com.titanium.product.valueobject.ReinsuranceConfig;
import com.titanium.product.valueobject.UnderwritingConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 产品模板查询服务实现（CQRS 读侧）
 * <p>
 * 查询由事件投影维护的读模型表 {@code t_product_template_view}，复杂配置字段从 JSON 反序列化还原为值对象。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductTemplateQueryServiceImpl implements ProductTemplateQueryService {

    private final ProductTemplateViewRepository templateViewRepository;

    @Override
    @Transactional(readOnly = true)
    public ProductTemplateQueryResult getTemplateById(String templateId, String tenantId) {
        return templateViewRepository.findByTemplateIdAndTenantId(templateId, tenantId)
                .map(this::toQueryResult)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductTemplateQueryResult getTemplateByProductId(String productId, String tenantId) {
        return templateViewRepository.findByProductIdAndTenantId(productId, tenantId)
                .map(this::toQueryResult)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductTemplateQueryResult getTemplateByCode(String templateCode, String tenantId) {
        return templateViewRepository.findByTemplateCodeAndTenantId(templateCode, tenantId)
                .map(this::toQueryResult)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductTemplateQueryResult> getTemplatesByInsuranceType(InsuranceType insuranceType, String tenantId) {
        return templateViewRepository.findByInsuranceTypeAndTenantId(insuranceType, tenantId)
                .stream()
                .map(this::toQueryResult)
                .toList();
    }

    /**
     * 读模型实体 → 查询结果 DTO，JSON 配置字段反序列化还原值对象
     */
    private ProductTemplateQueryResult toQueryResult(ProductTemplateView view) {
        return ProductTemplateQueryResult.builder()
                .templateId(view.getTemplateId())
                .templateCode(view.getTemplateCode())
                .templateName(view.getTemplateName())
                .insuranceCategory(view.getInsuranceCategory())
                .insuranceType(view.getInsuranceType())
                .productId(view.getProductId())
                .issuanceMode(view.getIssuanceMode())
                .policyStages(parseList(view.getPolicyStagesJson()))
                .underwritingConfig(parse(view.getUnderwritingConfigJson(), UnderwritingConfig.class))
                .policyStructure(parse(view.getPolicyStructureJson(), PolicyStructureConfig.class))
                .maintenanceConfig(parse(view.getMaintenanceConfigJson(), MaintenanceConfig.class))
                .claimConfig(parse(view.getClaimConfigJson(), ClaimConfig.class))
                .billingConfig(parse(view.getBillingConfigJson(), BillingConfig.class))
                .reinsuranceConfig(parse(view.getReinsuranceConfigJson(), ReinsuranceConfig.class))
                .dividendConfig(parse(view.getDividendConfigJson(), DividendConfig.class))
                .lifeProductSpec(parse(view.getLifeProductSpecJson(), LifeProductSpec.class))
                .status(view.getStatus())
                .tenantId(view.getTenantId())
                .build();
    }

    /**
     * JSON 字符串 → 值对象（null 安全）
     */
    private <T> T parse(String json, Class<T> type) {
        return json != null ? JSON.parseObject(json, type) : null;
    }

    /**
     * JSON 字符串 → 出单阶段列表（null 安全）
     */
    private List<PolicyStage> parseList(String json) {
        return json != null ? JSON.parseObject(json, new TypeReference<List<PolicyStage>>() {}) : null;
    }
}
