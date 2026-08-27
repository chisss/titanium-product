package com.titanium.product.infrastructure.pricing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 定价包锁定的税费策略版本证据。
 */
@Entity
@Table(name = "t_product_pricing_plan_tax_ref")
@Getter
@Setter
public class PricingPlanTaxPolicyRefEntity {

    @Id
    @Column(name = "ref_id", nullable = false, length = 36)
    private String refId;

    @Column(name = "plan_id", nullable = false, length = 36)
    private String planId;

    @Column(name = "policy_code", nullable = false, length = 64)
    private String policyCode;

    @Column(name = "policy_version", nullable = false, length = 32)
    private String policyVersion;

    @Column(name = "policy_hash", nullable = false, length = 64)
    private String policyHash;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
