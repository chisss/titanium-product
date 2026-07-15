package com.titanium.product.event;

import java.time.LocalDateTime;

import com.titanium.product.valueobject.LifeProductSpec;

/**
 * 寿险产品规格已配置事件
 * <p>
 * 产品模板成功配置寿险专属规格（投保年龄/保额范围/缴费期/保障期）时发布。读侧据此投影到
 * 产品模板读模型，供投保校验与保费计算查询寿险规格边界。
 * </p>
 *
 * @param templateId 产品模板ID
 * @param lifeProductSpec 寿险产品规格
 * @param tenantId 租户ID
 * @param occurredAt 事件发生时间
 */
public record LifeProductConfiguredEvent(String templateId, LifeProductSpec lifeProductSpec, String tenantId,
                                         LocalDateTime occurredAt) {
}
