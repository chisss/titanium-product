package com.titanium.product.command.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.titanium.product.common.enums.DynamicFactorMissingPolicy;
import com.titanium.product.common.enums.DynamicFactorSourceType;
import com.titanium.product.common.enums.DynamicFactorTransformType;
import com.titanium.product.common.enums.DynamicFactorValueTimePolicy;

/**
 * 创建动态因子草稿命令。
 */
public record CreateDynamicFactorCommand(
        String tenantId,
        String productId,
        String factorCode,
        String factorVersion,
        String factorName,
        String description,
        String featureCode,
        String featureDefinitionVersion,
        DynamicFactorSourceType sourceType,
        DynamicFactorValueTimePolicy valueTimePolicy,
        BigDecimal lowerBound,
        BigDecimal upperBound,
        DynamicFactorMissingPolicy missingPolicy,
        BigDecimal defaultValue,
        DynamicFactorTransformType transformType,
        BigDecimal multiplier,
        BigDecimal offset,
        boolean replayable,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo) {
}
