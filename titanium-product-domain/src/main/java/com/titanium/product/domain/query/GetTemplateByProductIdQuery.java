package com.titanium.product.domain.query;

/**
 * 根据产品ID查询产品模板
 */
public record GetTemplateByProductIdQuery(
        String productId,
        String tenantId
) {
}
