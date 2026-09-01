package com.titanium.product.api.request.premium;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建保单生命周期费用差额事实请求。
 */
public record PremiumLifecycleAdjustmentRequest(
        @NotBlank String adjustmentRequestId,
        @NotBlank String bizNo,
        @NotBlank String lifecycleType,
        @NotBlank String originalCalculationId,
        @NotBlank String replacementCalculationId,
        @NotNull LocalDateTime businessTime,
        @NotBlank String reason) {
}
