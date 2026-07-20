package com.titanium.product.event;

import java.util.List;

/**
 * 附加险关联更新事件
 * <p>
 * 当主险产品可搭配的附加险产品ID列表变更时发布。补齐此前 {@code UpdateAttachProductCommand}
 * 直接改字段未发事件的缺陷，保证事件溯源重放与读模型投影可感知该变更。
 * </p>
 *
 * @param productId        产品ID
 * @param attachProductIds 更新后的附加险产品ID列表
 */
public record ProductAttachUpdatedEvent(String productId, List<String> attachProductIds) {
}
