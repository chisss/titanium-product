package com.titanium.product.web.dto.maintenance;

import java.time.LocalDateTime;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 创建 Product 保全 Offering 草稿的后台请求。 */
public record CreateProductMaintenanceOfferingDTO(
        @NotBlank @Size(max = 64) String productVersion,
        @NotBlank @Size(max = 64) String planVersion,
        @NotBlank @Size(max = 64) String offeringVersion,
        @NotNull LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        @NotEmpty Set<@NotBlank String> allowedPolicyStatuses,
        @NotEmpty Set<@NotBlank String> allowedChannels,
        @NotEmpty Set<@NotBlank String> allowedItemCodes) {
}
