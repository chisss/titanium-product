package com.titanium.product.command;

import java.util.List;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import com.titanium.product.entity.ProductClauseRel;

/**
 * 更新产品条款关联命令 用于更新产品绑定的条款关系，仅允许在产品草稿状态时使用
 */
public record UpdateProductClauseRelCommand(@TargetAggregateIdentifier String productId,
                                            List<ProductClauseRel> newClauseRels) {
}
