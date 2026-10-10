package com.titanium.product.query.handler.query;

import java.util.List;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import com.titanium.product.query.query.FindCoverageSumInsuredByProductIdQuery;
import com.titanium.product.query.result.CoverageSumInsuredConfigQueryResult;
import com.titanium.product.query.service.CoverageSumInsuredQueryService;

import lombok.RequiredArgsConstructor;

/**
 * 责任级保额档次配置查询处理器（CQRS 读侧）
 * <p>
 * 薄查询处理器：接收按产品ID查询档次请求、委托 {@link CoverageSumInsuredQueryService} 查读模型
 * {@code t_product_coverage_sum_insured_view}。不含查询实现细节，复杂查询内聚在 query 层 QueryService。
 * </p>
 */
@Component
@RequiredArgsConstructor
@ProcessingGroup("product-query-group")
public class CoverageSumInsuredQueryHandler {

    private final CoverageSumInsuredQueryService coverageSumInsuredQueryService;

    /**
     * 处理按产品ID查询保额档次配置请求
     *
     * @param query 查询请求
     * @return 该产品全部责任的档次配置（未配置时为空列表）
     */
    @QueryHandler
    public List<CoverageSumInsuredConfigQueryResult> handle(FindCoverageSumInsuredByProductIdQuery query) {
        return coverageSumInsuredQueryService.findByProductId(query.productId());
    }
}
