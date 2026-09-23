package com.titanium.product.infrastructure.pricing.entity.calculation;

import java.time.LocalDateTime;

import com.titanium.product.common.enums.ActuarialDefinitionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

/**
 * Product 计算模型元数据实体。
 */
@Entity
@Table(name = "t_product_calculation_model")
@Getter
@Setter
public class CalculationModelDO {

    @Id
    @Column(name = "model_id", nullable = false, length = 36)
    private String modelId;
    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;
    @Column(name = "model_code", nullable = false, length = 64)
    private String modelCode;
    @Column(name = "model_version", nullable = false, length = 32)
    private String modelVersion;
    @Column(name = "model_name", nullable = false, length = 128)
    private String modelName;
    @Column(name = "description", length = 500)
    private String description;
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;
    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;
    @Column(name = "tenant_id", nullable = false, length = 32)
    private String tenantId;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ActuarialDefinitionStatus status;
    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;
    @Column(name = "create_time", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createTime;
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
