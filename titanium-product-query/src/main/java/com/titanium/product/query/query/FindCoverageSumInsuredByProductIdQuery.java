package com.titanium.product.query.query;

/**
 * 根据产品ID查询责任级保额档次配置
 * <p>
 * 读侧查询入参，命名遵循 CQRS 读侧约定（{@code FindXxxQuery}）。承保前置链路（G02 投保单录入）
 * 按「产品 + 责任」取档次清单即经本查询。
 * </p>
 *
 * @param productId 产品ID
 */
public record FindCoverageSumInsuredByProductIdQuery(String productId) {
}
