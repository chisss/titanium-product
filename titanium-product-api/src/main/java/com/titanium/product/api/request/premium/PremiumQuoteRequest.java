package com.titanium.product.api.request.premium;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Product 保费试算请求。
 */
public record PremiumQuoteRequest(
        @NotBlank String requestId,
        @NotNull LocalDateTime businessTime,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal sumInsured,
        @NotNull @Min(0) @Max(120) Integer age,
        @NotBlank String gender,
        @NotNull @Min(1) Integer paymentTermYears,
        @NotNull @Min(1) Integer coverageTermYears,
        @NotNull @Min(1) Integer paymentPeriods,
        Map<String, Object> requestSnapshot,
        String channelId,
        @Min(1) Integer policyYear) {

    public PremiumQuoteRequest(
            String requestId,
            LocalDateTime businessTime,
            String currency,
            BigDecimal sumInsured,
            Integer age,
            String gender,
            Integer paymentTermYears,
            Integer coverageTermYears,
            Integer paymentPeriods,
            Map<String, Object> requestSnapshot) {
        this(requestId, businessTime, currency, sumInsured, age, gender, paymentTermYears,
                coverageTermYears, paymentPeriods, requestSnapshot, null, 1);
    }
}
