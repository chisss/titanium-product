package com.titanium.product.application.command.pricing;

import java.time.LocalDateTime;
import java.util.List;

import com.titanium.product.common.enums.RateUnit;

/** 创建费率表草稿命令。 */
public record CreateRateTableDraftCommand(
        String tenantId,
        String productId,
        String tableCode,
        String tableVersion,
        RateUnit rateUnit,
        String currency,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        List<String> dimensionKeys) {
}
