package com.titanium.product.api.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Product 保全报价请求；最终金额和方向只能由 Product 计算。 */
public record MaintenancePremiumQuoteRequest(
        @NotBlank @Size(max = 64) String maintenanceId,
        @NotBlank @Size(max = 64) String policyId,
        @NotNull @Min(0) Long policyBaselineVersion,
        @NotBlank @Size(max = 64) String itemCode,
        @NotBlank @Size(max = 64) String productVersion,
        @NotBlank @Size(max = 64) String planVersion,
        @NotBlank @Size(max = 32) String lifecycleType,
        @NotNull @Valid SnapshotReferenceRequest beforeSnapshot,
        @NotNull @Valid SnapshotReferenceRequest proposedSnapshot,
        @NotBlank @Size(max = 128) String originalCalculationId,
        @NotNull LocalDateTime businessTime,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal sumInsured,
        @NotNull @Min(0) @Max(120) Integer age,
        @NotBlank @Size(max = 32) String gender,
        @NotNull @Min(1) Integer paymentTermYears,
        @NotNull @Min(1) Integer coverageTermYears,
        @NotNull @Min(1) Integer paymentPeriods,
        Map<String, Object> pricingFactors,
        @Valid List<UnderwritingAdjustmentRequest> underwritingAdjustments,
        @Size(max = 64) String channelId,
        @NotNull @Min(1) Integer policyYear,
        @NotBlank @Size(max = 500) String reason,
        @NotBlank @Size(max = 160) String idempotencyKey,
        @NotBlank @Pattern(regexp = "[a-fA-F0-9]{64}") String payloadHash) {

    public MaintenancePremiumQuoteRequest {
        pricingFactors = pricingFactors == null ? Map.of() : Map.copyOf(pricingFactors);
        underwritingAdjustments = underwritingAdjustments == null
                ? List.of()
                : List.copyOf(underwritingAdjustments);
    }

    /** 报价请求绑定的不可变案件快照引用。 */
    public record SnapshotReferenceRequest(
            @NotBlank @Size(max = 512) String storageKey,
            @NotBlank @Pattern(regexp = "[a-fA-F0-9]{64}") String contentHash,
            @NotNull @Min(0) Long policyVersion,
            @NotNull OffsetDateTime capturedAt) {
    }
}
