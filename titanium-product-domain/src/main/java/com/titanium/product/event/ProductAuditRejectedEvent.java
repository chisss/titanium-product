package com.titanium.product.event;

import java.time.LocalDateTime;

import org.axonframework.serialization.Revision;

import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.valueobject.AuditInfo;

/**
 * 产品审核驳回事件 当产品审核被驳回时发布
 */
@Revision("1")
public record ProductAuditRejectedEvent(String productId, ProductEnum.ProductStatus status, AuditInfo auditInfo,
                                        LocalDateTime rejectedAt) {
}
