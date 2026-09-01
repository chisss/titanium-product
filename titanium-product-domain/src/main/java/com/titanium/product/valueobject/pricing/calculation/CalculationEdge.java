package com.titanium.product.valueobject.pricing.calculation;

/**
 * 计算节点之间的有向依赖。
 */
public record CalculationEdge(String fromNodeCode, String toNodeCode) {

    public CalculationEdge {
        if (fromNodeCode == null || fromNodeCode.isBlank() || toNodeCode == null || toNodeCode.isBlank()
                || fromNodeCode.equals(toNodeCode)) {
            throw new IllegalArgumentException("计算依赖的起点和终点不合法");
        }
    }
}
