package com.titanium.product.domain.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * 复制产品命令
 * 基于已有产品快速复制创建新产品
 */
public record CopyProductCommand(
        @TargetAggregateIdentifier String newProductId,
        String sourceProductId,
        String newProductCode,
        String newProductName,
        String tenantId
) {
}
