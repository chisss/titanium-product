package com.titanium.product.event;

import java.time.LocalDateTime;

import org.axonframework.serialization.Revision;

/**
 * 产品模板激活事件
 */
@Revision("1")
public record ProductTemplateActivatedEvent(String templateId, String tenantId, LocalDateTime occurredAt) {
}
