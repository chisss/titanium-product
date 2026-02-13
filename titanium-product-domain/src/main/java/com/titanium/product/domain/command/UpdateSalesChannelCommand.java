package com.titanium.product.domain.command;

import java.util.List;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

import com.titanium.product.domain.valueobject.SalesChannelConfig;

/**
 * 更新销售渠道命令
 * 更新产品的销售渠道配置
 */
public record UpdateSalesChannelCommand(
        @TargetAggregateIdentifier String productId,
        List<SalesChannelConfig> salesChannels
) {
}
