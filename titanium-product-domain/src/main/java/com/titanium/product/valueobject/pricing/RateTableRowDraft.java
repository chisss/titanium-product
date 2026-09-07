package com.titanium.product.valueobject.pricing;

import java.math.BigDecimal;

/**
 * 待导入的费率行，不接受调用方指定内部行ID。
 *
 * @param occupationClass 职业类别（如意外险1-6类），null/ALL 表示通配
 * @param region 地区代码，null/ALL 表示通配
 * @param vehicleType 车型代码，null/ALL 表示通配
 */
public record RateTableRowDraft(
        Integer ageFrom,
        Integer ageToExclusive,
        String gender,
        Integer paymentTermYears,
        Integer coverageTermYears,
        BigDecimal rate,
        BigDecimal minimumPremium,
        BigDecimal maximumPremium,
        String occupationClass,
        String region,
        String vehicleType) {
}
