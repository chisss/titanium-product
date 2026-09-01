package com.titanium.product.valueobject.pricing.premium;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

import com.titanium.product.common.enums.PremiumBalanceDirection;

/** 单个追溯账务期间的 Product 价格差异。 */
public record RetroactivePremiumPeriodDifference(
        String periodId,
        String sourceReferenceId,
        LocalDateTime periodStart,
        BigDecimal originalAmount,
        BigDecimal recalculatedAmount,
        PremiumBalanceDirection direction,
        BigDecimal differenceAmount,
        String currency,
        String sourceEvidenceHash,
        String resultHash) {

    public RetroactivePremiumPeriodDifference {
        periodId = requireText(periodId, "期间ID");
        sourceReferenceId = requireText(sourceReferenceId, "来源引用");
        if (periodStart == null || originalAmount == null || originalAmount.signum() < 0
                || recalculatedAmount == null || recalculatedAmount.signum() < 0
                || direction == null || differenceAmount == null || differenceAmount.signum() < 0) {
            throw new IllegalArgumentException("期间时间、金额和方向不能为空或非法");
        }
        currency = requireText(currency, "币种").toUpperCase(Locale.ROOT);
        sourceEvidenceHash = requireHash(sourceEvidenceHash, "来源证据摘要");
        resultHash = requireHash(resultHash, "期间结果摘要");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "不能为空");
        }
        return value.trim();
    }

    private static String requireHash(String value, String field) {
        String hash = requireText(value, field).toLowerCase(Locale.ROOT);
        if (!hash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field + "必须为SHA-256");
        }
        return hash;
    }
}
