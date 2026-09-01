package com.titanium.product.api.request.premium;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 追溯保全按历史账务期间重新计算保费差额的请求。
 */
public record RetroactivePremiumPeriodRecalculationRequest(
        @NotBlank String recalculationRequestId,
        @NotBlank String maintenanceId,
        @NotBlank String policyId,
        @NotBlank String analysisId,
        @Min(1) int analysisVersion,
        @NotBlank @Pattern(regexp = "[0-9a-fA-F]{64}") String analysisResultHash,
        @NotBlank String originalCalculationId,
        @NotBlank String replacementCalculationId,
        @NotNull LocalDateTime scopeFrom,
        @NotNull LocalDateTime scopeTo,
        @NotNull @Size(max = 240) @Valid List<AffectedPeriodRequest> periods) {

    public RetroactivePremiumPeriodRecalculationRequest {
        periods = periods == null ? null : List.copyOf(periods);
    }

    /** Billing 权威历史期间基线，由 Product 计算替代金额和差额。 */
    public record AffectedPeriodRequest(
            @NotBlank String periodId,
            @NotBlank String sourceReferenceId,
            @NotNull LocalDateTime periodStart,
            @NotNull @DecimalMin("0") BigDecimal originalAmount,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
            @NotBlank @Pattern(regexp = "[0-9a-fA-F]{64}") String sourceEvidenceHash) {
    }
}
