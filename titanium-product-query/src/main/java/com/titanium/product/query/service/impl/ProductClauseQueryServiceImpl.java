package com.titanium.product.query.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import com.titanium.product.query.repository.ProductClauseRelViewRepository;
import com.titanium.product.query.result.ProductClauseQueryResult;
import com.titanium.product.query.service.ProductClauseQueryService;
import com.titanium.product.query.view.ProductClauseRelView;

import lombok.RequiredArgsConstructor;

/**
 * 产品条款关联查询服务实现（CQRS 读侧）
 * <p>
 * 读取读模型表 {@code t_product_clause_rel_view} 中以 JSON 存储的条款关联清单，反序列化后映射为查询结果 DTO。 显式按键名映射，兼容写侧领域实体
 * {@code ProductClauseRel} 的 {@code isMainClause} 命名。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ProductClauseQueryServiceImpl implements ProductClauseQueryService {

    private final ProductClauseRelViewRepository clauseRelViewRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductClauseQueryResult> findByProductId(String productId) {
        return clauseRelViewRepository.findById(productId)
                .map(this::toResults)
                .orElseGet(Collections::emptyList);
    }

    /**
     * 读模型 JSON 清单 → 查询结果 DTO 列表，按键名显式映射
     */
    private List<ProductClauseQueryResult> toResults(ProductClauseRelView view) {
        if (view.getClauseRelsJson() == null) {
            return Collections.emptyList();
        }
        JSONArray array = JSON.parseArray(view.getClauseRelsJson());
        List<ProductClauseQueryResult> results = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            ProductClauseQueryResult result = new ProductClauseQueryResult();
            result.setClauseId(item.getString("clauseId"));
            result.setClauseVersion(item.getString("clauseVersion"));
            result.setMainClause(item.getBoolean("isMainClause"));
            result.setBindTime(item.getObject("bindTime", java.time.LocalDateTime.class));
            results.add(result);
        }
        return results;
    }
}
