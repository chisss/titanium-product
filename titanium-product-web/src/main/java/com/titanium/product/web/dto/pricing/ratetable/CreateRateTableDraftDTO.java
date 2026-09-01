package com.titanium.product.web.dto.pricing.ratetable;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** 创建费率表草稿请求。 */
public record CreateRateTableDraftDTO(
        @NotBlank String tableCode,
        @NotBlank String tableVersion,
        @NotBlank String rateUnit,
        @NotBlank String currency,
        @NotNull LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        @NotEmpty List<@NotBlank String> dimensionKeys) {
}
