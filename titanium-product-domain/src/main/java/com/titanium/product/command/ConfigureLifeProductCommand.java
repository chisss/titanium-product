package com.titanium.product.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import com.titanium.product.valueobject.LifeProductSpec;

/**
 * 配置寿险产品规格命令
 * <p>
 * 为已创建的产品模板配置寿险专属规格（投保年龄/保额范围/缴费期/保障期选项）。区别于通用
 * {@code UpdateProductTemplateCommand}（配置出单/核保/理赔等行为）：本命令聚焦寿险产品的
 * 投保与核保规格边界，产出 {@code LifeProductConfiguredEvent}，供投保时校验年龄/保额、计费时约束缴费期。
 * </p>
 *
 * @param templateId 产品模板ID
 * @param lifeProductSpec 寿险产品规格
 * @param tenantId 租户ID
 */
public record ConfigureLifeProductCommand(@TargetAggregateIdentifier String templateId,
                                          LifeProductSpec lifeProductSpec, String tenantId) {
}
