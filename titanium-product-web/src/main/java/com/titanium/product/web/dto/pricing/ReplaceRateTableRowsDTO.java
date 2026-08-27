package com.titanium.product.web.dto.pricing;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/** 整体替换费率行请求。 */
public record ReplaceRateTableRowsDTO(@NotEmpty List<@Valid RateTableRowDTO> rows) {
}
