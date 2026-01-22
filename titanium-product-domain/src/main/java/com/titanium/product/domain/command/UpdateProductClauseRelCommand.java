package com.titanium.product.domain.command;

import com.titanium.product.domain.entity.ProductClauseRel;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.List;

/**
 * 更新产品条款关联命令
 * 用于更新产品绑定的条款关系，仅允许在产品草稿状态时使用
 */
public record UpdateProductClauseRelCommand(
        @TargetAggregateIdentifier String productId,
        List<ProductClauseRel> newClauseRels
) {
}