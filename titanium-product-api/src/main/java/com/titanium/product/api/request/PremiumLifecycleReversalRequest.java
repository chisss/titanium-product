package com.titanium.product.api.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 基于已确认生命周期差额生成反向差额事实的请求。 */
public record PremiumLifecycleReversalRequest(
        @NotBlank String adjustmentRequestId,
        @NotBlank String sourceAdjustmentId,
        @NotNull LocalDateTime businessTime,
        @NotBlank String reason) {
}
