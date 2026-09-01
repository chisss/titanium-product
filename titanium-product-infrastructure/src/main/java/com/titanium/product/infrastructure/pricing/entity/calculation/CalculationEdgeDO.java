package com.titanium.product.infrastructure.pricing.entity.calculation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 计算模型依赖边持久化实体。
 */
@Entity
@Table(name = "t_product_calculation_edge")
@Getter
@Setter
public class CalculationEdgeDO {

    @Id
    @Column(name = "edge_id", nullable = false, length = 36)
    private String edgeId;
    @Column(name = "model_id", nullable = false, length = 36)
    private String modelId;
    @Column(name = "from_node_code", nullable = false, length = 64)
    private String fromNodeCode;
    @Column(name = "to_node_code", nullable = false, length = 64)
    private String toNodeCode;
}
