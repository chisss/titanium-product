package com.titanium.product.command.pricing;

import java.time.LocalDateTime;
import java.util.List;

import com.titanium.product.valueobject.pricing.calculation.CalculationEdge;
import com.titanium.product.valueobject.pricing.calculation.CalculationNode;

/**
 * 创建结构化计算模型草稿命令。
 */
public record CreateCalculationModelCommand(
        String tenantId,
        String productId,
        String modelCode,
        String modelVersion,
        String modelName,
        String description,
        String currency,
        List<CalculationNode> nodes,
        List<CalculationEdge> edges,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo) {
}
