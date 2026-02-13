package com.titanium.product.domain.query;

/**
 * 根据模板ID查询产品模板
 */
public record GetTemplateByIdQuery(
        String templateId,
        String tenantId
) {
}
