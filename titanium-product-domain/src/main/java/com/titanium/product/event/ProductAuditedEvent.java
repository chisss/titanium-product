package com.titanium.product.event;

import java.time.LocalDateTime;

import org.axonframework.serialization.Revision;

import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.valueobject.AuditInfo;

/**
 * 产品审核通过事件 当产品审核通过并生效时发布
 */
@Revision("1")
public record ProductAuditedEvent(String productId, ProductEnum.ProductStatus status, LocalDateTime effectiveTime,
                                  AuditInfo auditInfo, String tenantId) {
}
