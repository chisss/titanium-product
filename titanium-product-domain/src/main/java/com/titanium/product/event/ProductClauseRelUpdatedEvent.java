package com.titanium.product.event;

import java.util.List;

import com.titanium.product.entity.ProductClauseRel;

/**
 * 产品条款关联更新事件 当产品的条款绑定关系发生变更时发布
 */
public record ProductClauseRelUpdatedEvent(String productId, List<ProductClauseRel> clauseRels) {
}
