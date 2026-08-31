package com.titanium.product.command.pricing.lifecycle;

import java.time.LocalDateTime;

import com.titanium.product.common.enums.PremiumLifecycleType;

/**
 * 创建不可变生命周期费用差额事实的应用命令。
 */
public record CreatePremiumLifecycleAdjustmentCommand(
        String tenantId,
        String adjustmentRequestId,
        String bizNo,
        PremiumLifecycleType lifecycleType,
        String originalCalculationId,
        String replacementCalculationId,
        LocalDateTime businessTime,
        String reason) {
}
