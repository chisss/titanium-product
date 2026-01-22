package com.titanium.product.domain.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * 审核产品命令
 * 用于审核产品，将产品状态从草稿或审核中变更为生效
 */
public record AuditProductCommand(
        @TargetAggregateIdentifier String productId
) {
}