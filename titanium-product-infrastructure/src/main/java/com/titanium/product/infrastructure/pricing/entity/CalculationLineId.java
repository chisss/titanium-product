package com.titanium.product.infrastructure.pricing.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Product 确认计算费用明细复合主键。
 */
@Embeddable
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class CalculationLineId implements Serializable {

    @Column(name = "calculation_id", nullable = false, length = 36)
    private String calculationId;

    @Column(name = "line_id", nullable = false, length = 64)
    private String lineId;
}
