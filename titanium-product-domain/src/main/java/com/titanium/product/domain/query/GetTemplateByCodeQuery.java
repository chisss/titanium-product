package com.titanium.product.domain.query;

/**
 * 根据模板编码查询产品模板
 */
public record GetTemplateByCodeQuery(
        String templateCode,
        String tenantId
) {
}
