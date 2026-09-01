package com.titanium.product.web.dto.pricing.ratetable;

import java.math.BigDecimal;

/** 费率行响应。 */
public record RateTableRowVO(
        String rowId,
        Integer ageFrom,
        Integer ageToExclusive,
        String gender,
        Integer paymentTermYears,
        Integer coverageTermYears,
        BigDecimal rate,
        BigDecimal minimumPremium,
        BigDecimal maximumPremium,
        String dimensionHash) {
}
