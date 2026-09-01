package com.titanium.product.valueobject.pricing.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import com.titanium.product.common.enums.PricingFeatureDataType;

/**
 * Product 侧类型化特征值联合结构。
 */
public record PricingFeatureValue(
        String featureCode,
        PricingFeatureDataType dataType,
        String status,
        String sourceType,
        String definitionVersion,
        LocalDateTime observedAt,
        List<String> qualityFlags,
        String stringValue,
        Long integerValue,
        BigDecimal decimalValue,
        Boolean booleanValue,
        LocalDate dateValue,
        LocalDateTime dateTimeValue,
        String enumValue,
        String jsonValue) {

    public PricingFeatureValue {
        if (featureCode == null || featureCode.isBlank()) {
            throw new IllegalArgumentException("featureCode不能为空");
        }
        Objects.requireNonNull(dataType, "dataType不能为空");
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status不能为空");
        }
        qualityFlags = qualityFlags == null ? List.of() : List.copyOf(qualityFlags);
        validateTypedPayload(
                dataType, status, featureCode, stringValue, integerValue, decimalValue, booleanValue,
                dateValue, dateTimeValue, enumValue, jsonValue);
    }

    private static void validateTypedPayload(
            PricingFeatureDataType dataType,
            String status,
            String featureCode,
            String stringValue,
            Long integerValue,
            BigDecimal decimalValue,
            Boolean booleanValue,
            LocalDate dateValue,
            LocalDateTime dateTimeValue,
            String enumValue,
            String jsonValue) {
        int valueCount = countValues(
                stringValue, integerValue, decimalValue, booleanValue, dateValue, dateTimeValue, enumValue, jsonValue);
        if (valueCount > 1) {
            throw new IllegalArgumentException("特征值联合结构只能包含一个值字段: " + featureCode);
        }
        if ("RESOLVED".equalsIgnoreCase(status) && valueCount != 1) {
            throw new IllegalArgumentException("已解析特征必须包含一个值字段: " + featureCode);
        }
        if (valueCount == 1 && !hasExpectedValueField(
                dataType, stringValue, integerValue, decimalValue, booleanValue, dateValue, dateTimeValue,
                enumValue, jsonValue)) {
            throw new IllegalArgumentException("特征值类型与值字段不匹配: " + featureCode);
        }
    }

    private static int countValues(
            String stringValue,
            Long integerValue,
            BigDecimal decimalValue,
            Boolean booleanValue,
            LocalDate dateValue,
            LocalDateTime dateTimeValue,
            String enumValue,
            String jsonValue) {
        return nonNull(stringValue) + nonNull(integerValue) + nonNull(decimalValue) + nonNull(booleanValue)
                + nonNull(dateValue) + nonNull(dateTimeValue) + nonNull(enumValue) + nonNull(jsonValue);
    }

    private static boolean hasExpectedValueField(
            PricingFeatureDataType dataType,
            String stringValue,
            Long integerValue,
            BigDecimal decimalValue,
            Boolean booleanValue,
            LocalDate dateValue,
            LocalDateTime dateTimeValue,
            String enumValue,
            String jsonValue) {
        return switch (dataType) {
            case STRING -> stringValue != null;
            case INTEGER -> integerValue != null;
            case DECIMAL -> decimalValue != null;
            case BOOLEAN -> booleanValue != null;
            case DATE -> dateValue != null;
            case DATETIME -> dateTimeValue != null;
            case ENUM -> enumValue != null;
            case JSON -> jsonValue != null;
        };
    }

    private static int nonNull(Object value) {
        return value == null ? 0 : 1;
    }
}
