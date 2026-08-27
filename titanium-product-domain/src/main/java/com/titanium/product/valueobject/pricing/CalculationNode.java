package com.titanium.product.valueobject.pricing;

import java.math.BigDecimal;

import com.titanium.product.common.enums.CalculationNodeType;
import com.titanium.product.common.enums.CalculationOperator;

/**
 * 计算模型的结构化节点。
 */
public record CalculationNode(
        String nodeCode,
        String nodeName,
        CalculationNodeType nodeType,
        CalculationOperator operator,
        String componentCode,
        String componentVersion,
        BigDecimal parameterValue,
        int executionOrder) {

    public CalculationNode {
        if (nodeCode == null || nodeCode.isBlank() || nodeName == null || nodeName.isBlank()
                || nodeType == null || operator == null || executionOrder < 0) {
            throw new IllegalArgumentException("计算节点编码、名称、类型、运算符和顺序不合法");
        }
        boolean componentComplete = componentCode != null && !componentCode.isBlank()
                && componentVersion != null && !componentVersion.isBlank();
        boolean componentEmpty = (componentCode == null || componentCode.isBlank())
                && (componentVersion == null || componentVersion.isBlank());
        if (!componentComplete && !componentEmpty) {
            throw new IllegalArgumentException("费用项编码和版本必须同时提供");
        }
        if ((operator == CalculationOperator.FIXED_AMOUNT || operator == CalculationOperator.PERCENTAGE_OF)
                && (parameterValue == null || parameterValue.signum() < 0)) {
            throw new IllegalArgumentException("固定金额或比例运算必须提供非负参数");
        }
    }

    public boolean hasComponent() {
        return componentCode != null && !componentCode.isBlank();
    }
}
