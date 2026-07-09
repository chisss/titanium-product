package com.titanium.product.query.handler.query;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import com.titanium.product.query.query.FindProductByIdQuery;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.service.ProductQueryService;

import lombok.RequiredArgsConstructor;

/**
 * 产品详情查询处理器（CQRS 读侧）
 * <p>
 * 薄查询处理器：接收 Axon 查询、委托 {@link ProductQueryService} 查读模型，不含查询实现细节。
 * </p>
 */
@Component
@RequiredArgsConstructor
@ProcessingGroup("product-query-group")
public class ProductDetailQueryHandler {

    private final ProductQueryService productQueryService;

    /**
     * 处理根据ID查询产品详情请求
     *
     * @param query 查询请求
     * @return 产品详情查询结果，不存在时返回 null
     */
    @QueryHandler
    public ProductQueryResult handle(FindProductByIdQuery query) {
        return productQueryService.findProductById(query.productId());
    }
}
