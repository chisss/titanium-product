package com.titanium.product.web.dto.pricing;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 费率行导入请求。 */
public record RateTableRowDTO(
        @Min(0) Integer ageFrom,
        @Min(1) Integer ageToExclusive,
        String gender,
        @Min(1) Integer paymentTermYears,
        @Min(1) Integer coverageTermYears,
        @NotNull @DecimalMin("0") BigDecimal rate,
        @DecimalMin("0") BigDecimal minimumPremium,
        @DecimalMin("0") BigDecimal maximumPremium) {
}
