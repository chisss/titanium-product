package com.titanium.product.api.response;

import java.time.OffsetDateTime;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

/** Product 保全 Offering 权威解析结果。 */
@Schema(description = "Product保全Offering权威解析结果")
public record ProductMaintenanceOfferingResolutionResponse(
        String tenantId,
        String productId,
        String productVersion,
        String planVersion,
        String offeringId,
        String offeringVersion,
        String contentHash,
        OffsetDateTime resolvedAt,
        Set<String> allowedItemCodes) {
}
