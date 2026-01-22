package com.titanium.product.domain.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * 下架产品命令
 * 用于将生效状态的产品下架
 */
public record InvalidateProductCommand(
        @TargetAggregateIdentifier String productId
) {
}