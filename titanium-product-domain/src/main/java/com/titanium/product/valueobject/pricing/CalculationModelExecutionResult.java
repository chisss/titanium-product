package com.titanium.product.valueobject.pricing;

import java.util.List;

/**
 * 结构化计算模型执行结果。
 */
public record CalculationModelExecutionResult(
        List<CalculationLine> lines,
        CalculationTotals totals,
        String outputNodeCode) {

    public CalculationModelExecutionResult {
        lines = lines == null ? List.of() : List.copyOf(lines);
        if (totals == null || outputNodeCode == null || outputNodeCode.isBlank()) {
            throw new IllegalArgumentException("计算模型执行结果不完整");
        }
    }
}
