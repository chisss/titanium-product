package com.titanium.product.query.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.product.query.repository.ProductTemplateViewRepository;
import com.titanium.product.query.repository.ProductViewRepository;
import com.titanium.product.query.result.ProductTemplateQueryResult;
import com.titanium.product.query.service.ProductTemplateQueryService;
import com.titanium.product.query.view.ProductTemplateView;
import com.titanium.product.query.view.ProductView;
import com.titanium.product.valueobject.LifeProductSpec;
import com.titanium.product.valueobject.PolicyStage;
import com.titanium.product.valueobject.config.BillingConfig;
import com.titanium.product.valueobject.config.ClaimConfig;
import com.titanium.product.valueobject.config.DividendConfig;
import com.titanium.product.valueobject.config.MaintenanceConfig;
import com.titanium.product.valueobject.config.PolicyStructureConfig;
import com.titanium.product.valueobject.config.ReinsuranceConfig;
import com.titanium.product.valueobject.config.UnderwritingConfig;

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
    private final ProductViewRepository         productViewRepository;

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
        // 产品→模板的关联落在产品读模型 t_product_view.template_id（一个模板被多个产品复用），
        // 故先据产品定位其绑定的 templateId，再取模板读模型；模板读模型自身的 product_id 列不可靠（投影未回填）。
        return productViewRepository.findByProductIdAndTenantId(productId, tenantId)
                .map(ProductView::getTemplateId)
                .filter(templateId -> templateId != null && !templateId.isBlank())
                .flatMap(templateId -> templateViewRepository.findByTemplateIdAndTenantId(templateId, tenantId))
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
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return JSON.parseObject(json, type);
        } catch (RuntimeException ex) {
            log.warn("产品模板配置 JSON 解析失败，忽略该配置: type={}, json={}", type.getSimpleName(), json, ex);
            return null;
        }
    }

    /**
     * JSON 字符串 → 出单阶段列表（null 安全）
     */
    private List<PolicyStage> parseList(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            Object parsed = JSON.parse(json);
            JSONArray values;
            if (parsed instanceof JSONArray array) {
                values = array;
            } else if (parsed instanceof JSONObject object) {
                values = object.getJSONArray("steps");
                if (values == null) {
                    return List.of();
                }
            } else {
                return List.of();
            }
            List<PolicyStage> stages = new ArrayList<>(values.size());
            for (Object value : values) {
                if (value instanceof String stageCode) {
                    stages.add(new PolicyStage(stageCode, stageCode, List.of(), null, null, false));
                } else if (value instanceof JSONObject object) {
                    stages.add(object.toJavaObject(PolicyStage.class));
                } else {
                    stages.add(JSON.parseObject(JSON.toJSONString(value), PolicyStage.class));
                }
            }
            return List.copyOf(stages);
        } catch (RuntimeException ex) {
            log.warn("产品模板出单阶段 JSON 解析失败，忽略阶段配置: json={}", json, ex);
            return List.of();
        }
    }
}
