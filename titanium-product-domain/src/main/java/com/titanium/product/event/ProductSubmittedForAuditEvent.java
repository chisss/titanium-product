package com.titanium.product.event;

import java.time.LocalDateTime;

import org.axonframework.serialization.Revision;

/**
 * 产品提交审核事件 当产品从DRAFT状态提交审核时发布
 */
@Revision("1")
public record ProductSubmittedForAuditEvent(String productId, String submitterId, String submitterName,
                                            LocalDateTime submittedAt) {
}
