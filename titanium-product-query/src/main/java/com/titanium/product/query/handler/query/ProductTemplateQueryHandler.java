package com.titanium.product.query.handler.query;

import java.util.List;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import com.titanium.product.query.query.GetTemplateByCodeQuery;
import com.titanium.product.query.query.GetTemplateByIdQuery;
import com.titanium.product.query.query.GetTemplateByProductIdQuery;
import com.titanium.product.query.query.GetTemplatesByInsuranceTypeQuery;
import com.titanium.product.query.result.ProductTemplateQueryResult;
import com.titanium.product.query.service.ProductTemplateQueryService;

import lombok.RequiredArgsConstructor;

/**
 * 产品模板查询处理器（CQRS 读侧）
 * <p>
 * 薄查询处理器：接收 Axon 查询、委托 {@link ProductTemplateQueryService} 查读模型，不含查询实现细节。
 * </p>
 */
@Component
@RequiredArgsConstructor
@ProcessingGroup("product-query-group")
public class ProductTemplateQueryHandler {

    private final ProductTemplateQueryService templateQueryService;

    @QueryHandler
    public ProductTemplateQueryResult handle(GetTemplateByIdQuery query) {
        return templateQueryService.getTemplateById(query.templateId(), query.tenantId());
    }

    @QueryHandler
    public ProductTemplateQueryResult handle(GetTemplateByProductIdQuery query) {
        return templateQueryService.getTemplateByProductId(query.productId(), query.tenantId());
    }

    @QueryHandler
    public ProductTemplateQueryResult handle(GetTemplateByCodeQuery query) {
        return templateQueryService.getTemplateByCode(query.templateCode(), query.tenantId());
    }

    @QueryHandler
    public List<ProductTemplateQueryResult> handle(GetTemplatesByInsuranceTypeQuery query) {
        return templateQueryService.getTemplatesByInsuranceType(query.insuranceType(), query.tenantId());
    }
}
