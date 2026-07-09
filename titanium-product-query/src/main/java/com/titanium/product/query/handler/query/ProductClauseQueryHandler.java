package com.titanium.product.query.handler.query;

import java.util.List;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import com.titanium.product.query.query.FindProductClauseByProductIdQuery;
import com.titanium.product.query.result.ProductClauseQueryResult;
import com.titanium.product.query.service.ProductClauseQueryService;

import lombok.RequiredArgsConstructor;

/**
 * 产品条款关联查询处理器（CQRS 读侧）
 * <p>
 * 薄查询处理器：接收按产品ID查询条款请求、委托 {@link ProductClauseQueryService} 查读模型
 * {@code t_product_clause_rel_view}。此前该查询无 QueryHandler，派发时会失败，本处理器补齐该缺口。
 * </p>
 */
@Component
@RequiredArgsConstructor
@ProcessingGroup("product-query-group")
public class ProductClauseQueryHandler {

    private final ProductClauseQueryService productClauseQueryService;

    /**
     * 处理按产品ID查询绑定条款请求
     *
     * @param query 查询请求
     * @return 条款关联查询结果列表
     */
    @QueryHandler
    public List<ProductClauseQueryResult> handle(FindProductClauseByProductIdQuery query) {
        return productClauseQueryService.findByProductId(query.productId());
    }
}
