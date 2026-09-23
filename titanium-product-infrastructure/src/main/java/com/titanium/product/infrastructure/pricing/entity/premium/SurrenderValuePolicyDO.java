package com.titanium.product.infrastructure.pricing.entity.premium;

import java.math.BigDecimal;
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

/** 退保价值策略持久化实体。 */
@Entity
@Table(name = "t_product_surrender_value_policy")
@Getter
@Setter
public class SurrenderValuePolicyDO {

    @Id
    @Column(name = "policy_id", nullable = false, length = 36)
    private String policyId;
    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;
    @Column(name = "policy_code", nullable = false, length = 64)
    private String policyCode;
    @Column(name = "policy_version", nullable = false, length = 32)
    private String policyVersion;
    @Column(name = "policy_year", nullable = false)
    private int policyYear;
    @Column(name = "cooling_off_days", nullable = false)
    private int coolingOffDays;
    @Column(name = "cash_value_rate", nullable = false, precision = 20, scale = 8)
    private BigDecimal cashValueRate;
    @Column(name = "internal_cost_retention_rate", nullable = false, precision = 20, scale = 8)
    private BigDecimal internalCostRetentionRate;
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
