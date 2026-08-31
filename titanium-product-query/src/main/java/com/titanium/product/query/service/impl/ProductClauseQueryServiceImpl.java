package com.titanium.product.query.service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;

import com.titanium.product.query.mapper.ProductClauseQueryResultMapper;
import com.titanium.product.query.repository.ProductClauseRelViewRepository;
import com.titanium.product.query.result.ProductClauseQueryResult;
import com.titanium.product.query.service.ProductClauseQueryService;
import com.titanium.product.query.view.ProductClauseRelView;

import lombok.RequiredArgsConstructor;

/**
 * 产品条款关联查询服务实现（CQRS 读侧）
 * <p>
 * 读取读模型表 {@code t_product_clause_rel_view} 中以 JSON 存储的条款关联清单，反序列化为防腐条目结构后经
 * {@link ProductClauseQueryResultMapper} 声明式映射为查询结果 DTO（兼容写侧领域实体 {@code ProductClauseRel} 的
 * {@code isMainClause} 命名）。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ProductClauseQueryServiceImpl implements ProductClauseQueryService {

    private final ProductClauseRelViewRepository clauseRelViewRepository;
    private final ProductClauseQueryResultMapper queryResultMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProductClauseQueryResult> findByProductId(String productId) {
        return clauseRelViewRepository.findById(productId)
                .map(this::toResults)
                .orElseGet(Collections::emptyList);
    }

    /**
     * 读模型 JSON 清单 → 查询结果 DTO 列表（防腐条目解析 + 声明式映射）
     */
    private List<ProductClauseQueryResult> toResults(ProductClauseRelView view) {
        if (view.getClauseRelsJson() == null) {
            return Collections.emptyList();
        }
        List<ProductClauseQueryResultMapper.ClauseRelItem> items =
                JSON.parseArray(view.getClauseRelsJson(), ProductClauseQueryResultMapper.ClauseRelItem.class);
        return items.stream().map(queryResultMapper::toQueryResult).toList();
    }
}
