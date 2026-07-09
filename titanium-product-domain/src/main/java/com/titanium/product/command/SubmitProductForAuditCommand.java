package com.titanium.product.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * 提交产品审核命令 将产品状态从DRAFT变更为AUDITING
 */
public record SubmitProductForAuditCommand(@TargetAggregateIdentifier String productId, String submitterId,
                                           String submitterName) {
}
