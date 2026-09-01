package com.titanium.product.web.dto.pricing.calculation;

import jakarta.validation.constraints.NotBlank;

/**
 * 结构化计算依赖请求。
 */
public record CalculationEdgeDTO(
        @NotBlank String fromNodeCode,
        @NotBlank String toNodeCode) {
}
