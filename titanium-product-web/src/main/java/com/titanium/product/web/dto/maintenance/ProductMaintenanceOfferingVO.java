package com.titanium.product.web.dto.maintenance;

import java.time.LocalDateTime;
import java.util.Set;

/** Product 保全 Offering 后台展示模型。 */
public record ProductMaintenanceOfferingVO(
        String offeringId,
        String productId,
        String productVersion,
        String planVersion,
        String offeringVersion,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        String status,
        String contentHash,
        Set<String> allowedPolicyStatuses,
        Set<String> allowedChannels,
        Set<String> allowedItemCodes) {
}
