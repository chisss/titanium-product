package com.titanium.product.event;

import java.time.LocalDateTime;

/**
 * 产品模板停用事件
 */
public record ProductTemplateDeactivatedEvent(String templateId, String tenantId, LocalDateTime occurredAt) {
}
