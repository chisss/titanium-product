package com.titanium.product.query.query;

/**
 * 根据产品ID查询绑定条款命令 用于查询指定产品绑定的条款列表
 */
public record FindProductClauseByProductIdQuery(String productId) {
}
