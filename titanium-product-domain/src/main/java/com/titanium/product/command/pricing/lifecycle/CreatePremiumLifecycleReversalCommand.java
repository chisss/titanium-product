package com.titanium.product.command.pricing.lifecycle;

import java.time.LocalDateTime;

/** 基于既有生命周期差额创建反向事实的应用命令。 */
public record CreatePremiumLifecycleReversalCommand(
        String tenantId,
        String adjustmentRequestId,
        String sourceAdjustmentId,
        LocalDateTime businessTime,
        String reason) {
}
