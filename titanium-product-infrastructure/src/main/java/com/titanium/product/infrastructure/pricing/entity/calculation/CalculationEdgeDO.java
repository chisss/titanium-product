package com.titanium.product.infrastructure.pricing.entity.calculation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
