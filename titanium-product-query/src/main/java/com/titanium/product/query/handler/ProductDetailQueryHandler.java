package com.titanium.product.query.handler;

import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;
import com.titanium.product.domain.query.FindProductByIdQuery;
import com.titanium.product.domain.valueobject.CoveragePeriodConfig;
import com.titanium.product.domain.valueobject.InsureCondition;
import com.titanium.product.domain.valueobject.IssuanceProcessConfig;
import com.titanium.product.domain.valueobject.PaymentConfig;
import com.titanium.product.domain.valueobject.PolicyFormConfig;
import com.titanium.product.domain.valueobject.PricingBasicRule;
import com.titanium.product.domain.valueobject.UnderwritingConfig;
import com.titanium.product.query.entity.ProductView;
import com.titanium.product.query.repository.ProductViewRepository;
import com.titanium.product.query.entity.ProductQueryResult;

import lombok.RequiredArgsConstructor;

/**
 * 产品详情查询处理器（CQRS 读侧）
 * <p>
 * 改造说明：原实现通过 {@code ProductRepository.findById()} 重建写侧事件溯源聚合根来查询， 严重违背 CQRS
 * 读写分离原则。现改为查询由事件投影维护的读模型表 {@code t_product_view}， 复杂配置字段从 JSON 反序列化还原。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class ProductDetailQueryHandler {

    private final ProductViewRepository productViewRepository;

    /**
     * 处理根据ID查询产品详情请求
     *
     * @param query 查询请求
     * @return 产品详情查询结果，不存在时返回 null
     */
    @QueryHandler
    @Transactional(readOnly = true)
    public ProductQueryResult handle(FindProductByIdQuery query) {
        return productViewRepository.findById(query.productId())
                .map(this::toQueryResult)
                .orElse(null);
    }

    /**
     * 读模型实体 → 查询结果 DTO，JSON 配置字段反序列化还原值对象
     */
    private ProductQueryResult toQueryResult(ProductView view) {
        ProductQueryResult result = new ProductQueryResult();
        result.setProductId(view.getProductId());
        result.setProductCode(view.getProductCode());
        result.setProductName(view.getProductName());
        result.setProductDesc(view.getProductDesc());
        result.setForm(view.getForm());
        result.setInsuranceType(view.getInsuranceType());
        result.setCategory(view.getCategory());
        result.setVersion(view.getVersionNo());
        result.setStatus(view.getStatus());
        result.setOriginalProductId(view.getOriginalProductId());
        result.setEffectiveTime(view.getEffectiveTime());
        result.setInvalidTime(view.getInvalidTime());
        result.setSaleStartTime(view.getSaleStartTime());
        result.setSaleEndTime(view.getSaleEndTime());
        result.setInsureCondition(parse(view.getInsureConditionJson(), InsureCondition.class));
        result.setCoveragePeriod(parse(view.getCoveragePeriodJson(), CoveragePeriodConfig.class));
        result.setPaymentConfig(parse(view.getPaymentConfigJson(), PaymentConfig.class));
        result.setPricingBasicRule(parse(view.getPricingBasicRuleJson(), PricingBasicRule.class));
        result.setIssuanceProcessConfig(parse(view.getIssuanceProcessConfigJson(), IssuanceProcessConfig.class));
        result.setPolicyFormConfig(parse(view.getPolicyFormConfigJson(), PolicyFormConfig.class));
        result.setUnderwritingConfig(parse(view.getUnderwritingConfigJson(), UnderwritingConfig.class));
        result.setCreatedAt(view.getCreatedAt());
        result.setTenantId(view.getTenantId());
        return result;
    }

    /**
     * JSON 字符串 → 值对象（null 安全）
     */
    private <T> T parse(String json, Class<T> type) {
        return json != null ? JSON.parseObject(json, type) : null;
    }
}
