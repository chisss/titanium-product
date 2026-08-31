package com.titanium.product.infrastructure.pricing.entity;

import java.math.BigDecimal;

import com.titanium.product.common.enums.CalculationNodeType;
import com.titanium.product.common.enums.CalculationOperator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 计算模型节点持久化实体。
 */
@Entity
@Table(name = "t_product_calculation_node")
@Getter
@Setter
public class CalculationNodeDO {

    @Id
    @Column(name = "node_id", nullable = false, length = 36)
    private String nodeId;
    @Column(name = "model_id", nullable = false, length = 36)
    private String modelId;
    @Column(name = "node_code", nullable = false, length = 64)
    private String nodeCode;
    @Column(name = "node_name", nullable = false, length = 128)
    private String nodeName;
    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false, length = 16)
    private CalculationNodeType nodeType;
    @Enumerated(EnumType.STRING)
    @Column(name = "operator_type", nullable = false, length = 32)
    private CalculationOperator operator;
    @Column(name = "component_code", length = 64)
    private String componentCode;
    @Column(name = "component_version", length = 32)
    private String componentVersion;
    @Column(name = "parameter_value", precision = 20, scale = 8)
    private BigDecimal parameterValue;
    @Column(name = "execution_order", nullable = false)
    private int executionOrder;
}
