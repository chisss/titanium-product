package com.titanium.product.infrastructure.pricing.entity.premium;

import java.time.LocalDateTime;

import com.titanium.product.common.enums.RateTableStatus;
import com.titanium.product.common.enums.RateUnit;

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
 * Product 自有费率表元数据实体。
 */
@Entity
@Table(name = "t_product_rate_table")
@Getter
@Setter
public class RateTableDO {

    @Id
    @Column(name = "table_id", nullable = false, length = 36)
    private String tableId;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(name = "table_code", nullable = false, length = 64)
    private String tableCode;

    @Column(name = "table_version", nullable = false, length = 32)
    private String tableVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private RateTableStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_unit", nullable = false, length = 32)
    private RateUnit rateUnit;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "dimension_keys_json", nullable = false, columnDefinition = "TEXT")
    private String dimensionKeysJson;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "row_count", nullable = false)
    private long rowCount;

    @Column(name = "tenant_id", nullable = false, length = 32)
    private String tenantId;

    @Column(name = "create_time", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updateTime;
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
