package com.titanium.product.event;

import java.time.LocalDateTime;

import org.axonframework.serialization.Revision;

/**
 * 产品模板停用事件
 */
@Revision("1")
public record ProductTemplateDeactivatedEvent(String templateId, String tenantId, LocalDateTime occurredAt) {
}
