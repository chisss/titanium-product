package com.titanium.product.valueobject.pricing;

import java.math.BigDecimal;

/** 待导入的费率行，不接受调用方指定内部行ID。 */
public record RateTableRowDraft(
        Integer ageFrom,
        Integer ageToExclusive,
        String gender,
        Integer paymentTermYears,
        Integer coverageTermYears,
        BigDecimal rate,
        BigDecimal minimumPremium,
        BigDecimal maximumPremium) {
}
