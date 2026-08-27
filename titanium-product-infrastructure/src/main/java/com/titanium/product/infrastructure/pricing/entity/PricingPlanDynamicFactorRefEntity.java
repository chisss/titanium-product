package com.titanium.product.infrastructure.pricing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** 定价包锁定的动态因子版本证据。 */
@Entity
@Table(name = "t_product_pricing_plan_dynamic_factor_ref")
@Getter
@Setter
public class PricingPlanDynamicFactorRefEntity {

    @Id
    @Column(name = "ref_id", nullable = false, length = 36)
    private String refId;
    @Column(name = "plan_id", nullable = false, length = 36)
    private String planId;
    @Column(name = "factor_code", nullable = false, length = 64)
    private String factorCode;
    @Column(name = "factor_version", nullable = false, length = 32)
    private String factorVersion;
    @Column(name = "factor_hash", nullable = false, length = 64)
    private String factorHash;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
