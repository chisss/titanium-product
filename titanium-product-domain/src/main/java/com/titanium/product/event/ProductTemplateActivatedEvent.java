package com.titanium.product.event;

import java.time.LocalDateTime;

/**
 * 产品模板激活事件
 */
public record ProductTemplateActivatedEvent(String templateId, String tenantId, LocalDateTime occurredAt) {
}
