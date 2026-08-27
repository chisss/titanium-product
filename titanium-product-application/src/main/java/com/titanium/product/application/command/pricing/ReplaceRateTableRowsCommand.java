package com.titanium.product.application.command.pricing;

import java.util.List;

/** 整体替换费率表草稿行命令。 */
public record ReplaceRateTableRowsCommand(
        String tenantId,
        String productId,
        String tableId,
        List<RateTableRowDraft> rows) {
}
