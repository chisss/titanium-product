package com.titanium.product.command.pricing;

import java.util.List;

import com.titanium.product.valueobject.pricing.RateTableRowDraft;

/** 整体替换费率表草稿行命令。 */
public record ReplaceRateTableRowsCommand(
        String tenantId,
        String productId,
        String tableId,
        List<RateTableRowDraft> rows) {
}
