package com.titanium.product.api.response.premium;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Product 不可变生命周期费用差额事实响应。
 */
public record PremiumLifecycleAdjustmentResponse(
        String adjustmentId,
        String adjustmentRequestId,
        String bizNo,
        String lifecycleType,
        String productId,
        String originalCalculationId,
        String originalResultHash,
        String replacementCalculationId,
        String replacementResultHash,
        LocalDateTime businessTime,
        String currency,
        String direction,
        BigDecimal customerAmount,
        String taxDirection,
        BigDecimal taxAmount,
        String internalCostDirection,
        BigDecimal internalCostAmount,
        List<PremiumLifecycleDifferenceLineResponse> lines,
        String reason,
        String requestHash,
        String resultHash,
        LocalDateTime createdAt,
        String reversalOfAdjustmentId) {
}
