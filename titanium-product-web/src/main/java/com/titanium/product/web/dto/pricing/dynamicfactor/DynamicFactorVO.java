package com.titanium.product.web.dto.pricing.dynamicfactor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 动态因子后台响应。 */
public record DynamicFactorVO(
        String factorId,
        String productId,
        String factorCode,
        String factorVersion,
        String factorName,
        String description,
        String featureCode,
        String featureDefinitionVersion,
        String sourceType,
        String valueTimePolicy,
        BigDecimal lowerBound,
        BigDecimal upperBound,
        String missingPolicy,
        BigDecimal defaultValue,
        String transformType,
        BigDecimal multiplier,
        BigDecimal offset,
        boolean replayable,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        String status,
        String contentHash) {
}
