package com.titanium.product.event;

import java.util.List;

import org.axonframework.serialization.Revision;

import com.titanium.product.valueobject.config.SalesChannelConfig;

/**
 * 产品销售渠道更新事件 当产品的销售渠道配置发生变更时发布
 */
@Revision("1")
public record ProductSalesChannelUpdatedEvent(String productId, List<SalesChannelConfig> salesChannels) {
}
