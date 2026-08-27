package com.titanium.product.query.handler.query;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.titanium.product.query.query.FindProductByConditionQuery;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.service.ProductQueryService;

import lombok.RequiredArgsConstructor;

/**
 * 产品条件查询处理器（CQRS 读侧）
 * <p>
 * 薄查询处理器：接收多条件分页查询、委托 {@link ProductQueryService} 查读模型 {@code t_product_view}，
 * 不含查询实现细节。此前该查询无 QueryHandler，派发时会失败，本处理器补齐该缺口。
 * </p>
 */
@Component
@RequiredArgsConstructor
@ProcessingGroup("product-query-group")
public class ProductConditionQueryHandler {

    private final ProductQueryService productQueryService;

    /**
     * 处理按条件分页查询产品列表请求
     *
     * @param query 查询请求（形态/险种/状态任意组合 + 分页）
     * @return 分页产品查询结果
     */
    @QueryHandler
    public Page<ProductQueryResult> handle(FindProductByConditionQuery query) {
        return productQueryService.findByCondition(
                query.productName(), query.form(), query.type(), query.status(), query.pageNum(), query.pageSize(),
                query.tenantId());
    }
}
