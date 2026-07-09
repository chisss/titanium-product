package com.titanium.product.query.service;

import java.util.List;

import com.titanium.product.query.result.ProductClauseQueryResult;

/**
 * 产品条款关联查询服务（CQRS 读侧）
 * <p>
 * 查询由事件投影维护的读模型表 {@code t_product_clause_rel_view}，返回产品绑定的条款清单。
 * </p>
 */
public interface ProductClauseQueryService {

    /**
     * 查询产品绑定的条款关联清单
     *
     * @param productId 产品ID
     * @return 条款关联查询结果列表，无绑定时返回空列表
     */
    List<ProductClauseQueryResult> findByProductId(String productId);
}
