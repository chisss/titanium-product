package com.titanium.product.api.request.premium;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 基于原确认计算确认退保价值的请求。 */
public record SurrenderValueCalculationRequest(
        @NotBlank String surrenderRequestId,
        @NotBlank String bizNo,
        @NotBlank String originalBizNo,
        @NotBlank String originalCalculationId,
        @NotNull LocalDate policyEffectiveDate,
        @NotNull LocalDate surrenderDate,
        @NotNull @Min(1) Integer policyYear,
        @NotNull LocalDateTime businessTime,
        @NotBlank String reason) {
}
