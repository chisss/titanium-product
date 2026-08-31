package com.titanium.product.query.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.metadata.enums.insurance.InsuranceProductType;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.query.mapper.ProductQueryResultMapper;
import com.titanium.product.query.repository.ProductViewRepository;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.service.ProductQueryService;
import com.titanium.product.query.view.ProductView;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 产品查询服务实现（CQRS 读侧）
 * <p>
 * 查询由事件投影维护的读模型表 {@code t_product_view}，复杂配置字段从 JSON 反序列化还原为值对象（经
 * {@link ProductQueryResultMapper} 声明式映射）。 原 QueryHandler 通过重建写侧事件溯源聚合根来查询、严重违背读写分离的缺陷已在此彻底根除。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductQueryServiceImpl implements ProductQueryService {

    private final ProductViewRepository productViewRepository;
    private final ProductQueryResultMapper queryResultMapper;

    @Override
    @Transactional(readOnly = true)
    public ProductQueryResult findProductById(String productId) {
        return productViewRepository.findById(productId)
                .map(queryResultMapper::toQueryResult)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductQueryResult findProductById(String productId, String tenantId) {
        return productViewRepository.findByProductIdAndTenantId(productId, tenantId)
                .map(queryResultMapper::toQueryResult)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductQueryResult findProductByCode(String productCode, String tenantId) {
        return productViewRepository.findByProductCodeAndTenantId(productCode, tenantId)
                .map(queryResultMapper::toQueryResult)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductQueryResult> findByCondition(String productName, ProductEnum.ProductForm form,
                                                    InsuranceProductType type, ProductEnum.ProductStatus status,
                                                    int pageNum, int pageSize, String tenantId) {
        Pageable pageable = PageRequest.of(pageNum, pageSize);
        Specification<ProductView> spec = buildConditionSpec(productName, form, type, status, tenantId);
        return productViewRepository.findAll(spec, pageable).map(queryResultMapper::toQueryResult);
    }

    /**
     * 组装多条件动态查询规格：产品名称（模糊）/形态/险种/状态任意组合，非空条件以 AND 结合
     */
    private Specification<ProductView> buildConditionSpec(String productName, ProductEnum.ProductForm form,
                                                          InsuranceProductType type, ProductEnum.ProductStatus status,
                                                          String tenantId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (productName != null && !productName.isBlank()) {
                predicates.add(cb.like(root.get("productName"), "%" + productName.trim() + "%"));
            }
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
}
