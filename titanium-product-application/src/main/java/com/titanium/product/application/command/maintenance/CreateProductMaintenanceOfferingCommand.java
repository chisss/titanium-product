package com.titanium.product.application.command.maintenance;

import java.time.LocalDateTime;
import java.util.Set;

/** 创建 Product 保全 Offering 草稿的应用命令。 */
public record CreateProductMaintenanceOfferingCommand(
        String tenantId,
        String productId,
        String productVersion,
        String planVersion,
        String offeringVersion,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        Set<String> allowedPolicyStatuses,
        Set<String> allowedChannels,
        Set<String> allowedItemCodes) {
}
