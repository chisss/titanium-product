package com.titanium.product.web.dto.pricing.calculation;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 创建计算模型草稿请求。
 */
public record CreateCalculationModelDTO(
        @NotBlank String modelCode,
        @NotBlank String modelVersion,
        @NotBlank String modelName,
        String description,
        @NotBlank String currency,
        @NotEmpty List<@Valid CalculationNodeDTO> nodes,
        List<@Valid CalculationEdgeDTO> edges,
        @NotNull LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo) {
}
