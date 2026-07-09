package com.titanium.product.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * 驳回产品审核命令 将产品状态从AUDITING变更回DRAFT
 */
public record RejectProductAuditCommand(@TargetAggregateIdentifier String productId, String auditorId,
                                        String auditorName, String rejectReason) {
}
