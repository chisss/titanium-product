package com.titanium.product.infrastructure.pricing.entity.calculation;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Product 确认计算结构化汇总持久化实体。
 */
@Entity
@Table(name = "t_product_calculation_total")
@Getter
@Setter
public class CalculationTotalDO {

    @Id
    @Column(name = "calculation_id", nullable = false, length = 36)
    private String calculationId;

    @Column(name = "premium_subtotal", nullable = false, precision = 20, scale = 8)
    private BigDecimal premiumSubtotal;

    @Column(name = "tax_and_levy_total", nullable = false, precision = 20, scale = 8)
    private BigDecimal taxAndLevyTotal;

    @Column(name = "customer_payable", nullable = false, precision = 20, scale = 8)
    private BigDecimal customerPayable;

    @Column(name = "internal_cost_total", nullable = false, precision = 20, scale = 8)
    private BigDecimal internalCostTotal;
}
