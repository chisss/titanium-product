package com.titanium.product.command;

import java.util.List;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import com.titanium.product.entity.ProductClauseRel;

/**
 * 更新产品条款关联命令 用于更新产品绑定的条款关系，仅允许在产品草稿状态时使用
 *
 * <p>🔴 {@code tenantId} 是**校验必需**而非冗余携带：条款绑定的真伪与状态须向条款域求证
 * （{@code ClauseExistencePort}），而该调用是跨服务查询、必须带租户上下文。本命令此前没有该字段，
 * 导致这条路径**无法完成跨域校验**（R7-02 登记遗留，2026-09-22 随 R8-01 补齐）。
 * 与 {@link CreateProductCommand} 一致，租户随命令贯穿（项目规约 §5.1）。</p>
 *
 * @param productId     产品ID（聚合标识）
 * @param newClauseRels 待替换的条款关联列表（整体替换语义）
 * @param tenantId      租户ID
 */
public record UpdateProductClauseRelCommand(@TargetAggregateIdentifier String productId,
                                            List<ProductClauseRel> newClauseRels,
                                            String tenantId) {
}
