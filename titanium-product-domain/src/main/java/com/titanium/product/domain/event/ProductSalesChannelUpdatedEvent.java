package com.titanium.product.domain.event;

import java.util.List;

import com.titanium.product.domain.valueobject.SalesChannelConfig;

/**
 * 产品销售渠道更新事件
 * 当产品的销售渠道配置发生变更时发布
 */
public record ProductSalesChannelUpdatedEvent(
        String productId,
        List<SalesChannelConfig> salesChannels
) {
}
