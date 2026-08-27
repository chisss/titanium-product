package com.titanium.product.web.dto.pricing;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 结构化计算节点请求。
 */
public record CalculationNodeDTO(
        @NotBlank String nodeCode,
        @NotBlank String nodeName,
        @NotBlank String nodeType,
        @NotBlank String operator,
        String componentCode,
        String componentVersion,
        BigDecimal parameterValue,
        @Min(0) int executionOrder) {
}
