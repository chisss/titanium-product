package com.titanium.product.domain.event;

/**
 * 产品模板激活事件
 */
public record ProductTemplateActivatedEvent(
        String templateId,
        String tenantId
) {
}
