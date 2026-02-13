package com.titanium.product.domain.event;

import java.time.LocalDateTime;

/**
 * 产品提交审核事件
 * 当产品从DRAFT状态提交审核时发布
 */
public record ProductSubmittedForAuditEvent(
        String productId,
        String submitterId,
        String submitterName,
        LocalDateTime submittedAt
) {
}
