package com.titanium.product.infrastructure.pricing.entity.paidup;

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

/** 减额缴清净费率策略持久化实体（g04-05）。 */
@Entity
@Table(name = "t_product_reduced_paid_up_policy")
@Getter
@Setter
public class ReducedPaidUpPolicyDO {

    @Id
    @Column(name = "policy_id", nullable = false, length = 36)
    private String policyId;
    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;
    @Column(name = "policy_code", nullable = false, length = 64)
    private String policyCode;
    @Column(name = "policy_version", nullable = false, length = 32)
    private String policyVersion;
    @Column(name = "net_single_premium_rate_per_unit", nullable = false, precision = 20, scale = 8)
    private BigDecimal netSinglePremiumRatePerUnit;
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
