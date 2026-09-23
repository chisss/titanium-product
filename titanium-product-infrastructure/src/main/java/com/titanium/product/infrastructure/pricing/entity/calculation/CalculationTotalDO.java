package com.titanium.product.infrastructure.pricing.entity.calculation;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
    /**
     * 乐观锁版本（JPA 自动维护）。
     * <p>
     * m20-09（审查报告 PER-01）补齐：原先写侧 DO 无 {@code @Version}，并发更新走无条件 UPDATE，
     * 后写者静默覆盖前写者、数据丢失。加此字段后 JPA 的 UPDATE 会带 {@code WHERE version = ?}，
     * 冲突时抛 {@code ObjectOptimisticLockingFailureException}（由异常处理器映射为 409）。
     * </p>
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

}
