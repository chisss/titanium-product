package com.titanium.product.valueobject;

import java.math.BigDecimal;

import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 销售渠道配置值对象 定义产品的销售渠道及佣金比例
 *
 * @param channelType 渠道类型
 * @param enabled 是否启用
 * @param commissionRate 佣金比例
 */
public record SalesChannelConfig(ProductEnum.SalesChannel channelType, boolean enabled, BigDecimal commissionRate) {
}
