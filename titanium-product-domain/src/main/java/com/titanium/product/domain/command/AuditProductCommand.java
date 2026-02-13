package com.titanium.product.domain.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 审核产品命令
 * 用于审核产品，包含审核人信息和审核结果
 */
public record AuditProductCommand(
        @TargetAggregateIdentifier String productId,
        String auditorId,
        String auditorName,
        String auditOpinion,
        ProductEnum.AuditResult auditResult
) {
}
