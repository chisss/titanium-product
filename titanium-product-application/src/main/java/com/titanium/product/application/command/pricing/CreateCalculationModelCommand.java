package com.titanium.product.application.command.pricing;

import java.time.LocalDateTime;
import java.util.List;

import com.titanium.product.valueobject.pricing.CalculationEdge;
import com.titanium.product.valueobject.pricing.CalculationNode;

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
