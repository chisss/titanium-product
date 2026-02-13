package com.titanium.product.domain.event;

/**
 * 产品模板停用事件
 */
public record ProductTemplateDeactivatedEvent(
        String templateId,
        String tenantId
) {
}
