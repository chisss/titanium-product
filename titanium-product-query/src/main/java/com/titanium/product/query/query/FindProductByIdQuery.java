package com.titanium.product.query.query;

/**
 * 根据ID查询产品命令 用于根据产品ID查询产品详情
 */
public record FindProductByIdQuery(String productId) {
}
