package com.titanium.product.web.dto.pricing.ratetable;

import java.time.LocalDateTime;
import java.util.List;

/** 费率表版本及明细响应。 */
public record RateTableVO(
        String tableId,
        String productId,
        String tableCode,
        String tableVersion,
        String status,
        String rateUnit,
        String currency,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        List<String> dimensionKeys,
        long rowCount,
        String contentHash,
        List<RateTableRowVO> rows) {
}
