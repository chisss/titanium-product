package com.titanium.product.web.dto.pricing;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 计算模型后台响应。
 */
public record CalculationModelVO(
        String modelId,
        String productId,
        String modelCode,
        String modelVersion,
        String modelName,
        String description,
        String currency,
        List<CalculationNodeDTO> nodes,
        List<CalculationEdgeDTO> edges,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        String status,
        String contentHash) {
}
