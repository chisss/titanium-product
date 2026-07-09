package com.titanium.product.command;

import java.util.List;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * 更新附加险关联命令 更新主险产品可搭配的附加险产品ID列表
 */
public record UpdateAttachProductCommand(@TargetAggregateIdentifier String productId, List<String> attachProductIds) {
}
