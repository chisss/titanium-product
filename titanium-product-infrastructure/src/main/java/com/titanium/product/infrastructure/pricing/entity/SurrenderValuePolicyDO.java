package com.titanium.product.infrastructure.pricing.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.titanium.product.common.enums.ActuarialDefinitionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
}
