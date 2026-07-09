package com.titanium.product.query.service.impl;


import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.query.repository.ProductViewRepository;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.service.ProductQueryService;
import com.titanium.product.query.view.ProductView;
import com.titanium.product.valueobject.CoveragePeriodConfig;
import com.titanium.product.valueobject.InsureCondition;
import com.titanium.product.valueobject.IssuanceProcessConfig;
import com.titanium.product.valueobject.PaymentConfig;
import com.titanium.product.valueobject.PolicyFormConfig;
import com.titanium.product.valueobject.PricingBasicRule;
import com.titanium.product.valueobject.UnderwritingConfig;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 产品查询服务实现（CQRS 读侧）
 * <p>
 * 查询由事件投影维护的读模型表 {@code t_product_view}，复杂配置字段从 JSON 反序列化还原为值对象。 原
 * QueryHandler 通过重建写侧事件溯源聚合根来查询、严重违背读写分离的缺陷已在此彻底根除。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductQueryServiceImpl implements ProductQueryService {

    private final ProductViewRepository productViewRepository;

    @Override
    @Transactional(readOnly = true)
    public ProductQueryResult findProductById(String productId) {
        return productViewRepository.findById(productId)
                .map(this::toQueryResult)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductQueryResult> findByCondition(ProductEnum.ProductForm form, InsuranceType type,
                                                    ProductEnum.ProductStatus status, int pageNum, int pageSize) {
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Specification<ProductView> spec = buildConditionSpec(form, type, status);
        return productViewRepository.findAll(spec, pageable).map(this::toQueryResult);
    }

    /**
     * 组装多条件动态查询规格：形态/险种/状态任意组合，非空条件以 AND 结合
     */
    private Specification<ProductView> buildConditionSpec(ProductEnum.ProductForm form, InsuranceType type,
                                                          ProductEnum.ProductStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (form != null) {
                predicates.add(cb.equal(root.get("form"), form));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("insuranceType"), type));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
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
