package com.titanium.product.valueobject;

import java.time.LocalDateTime;
import java.util.List;

import com.titanium.product.common.enums.RateUnit;

/**
 * 已发布费率表的不可变运行时快照。
 */
public record RateTableSnapshot(
        String tableId,
        String productId,
        String tableCode,
        String tableVersion,
        RateUnit rateUnit,
        String currency,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        String contentHash,
        List<RateTableRow> candidateRows) {

    public RateTableSnapshot {
        candidateRows = candidateRows == null ? List.of() : List.copyOf(candidateRows);
    }

    /**
     * 业务时点是否位于左闭右开的有效期内。
     */
    public boolean isEffectiveAt(LocalDateTime businessTime) {
        return businessTime != null
                && (effectiveFrom == null || !businessTime.isBefore(effectiveFrom))
                && (effectiveTo == null || businessTime.isBefore(effectiveTo));
    }
}
