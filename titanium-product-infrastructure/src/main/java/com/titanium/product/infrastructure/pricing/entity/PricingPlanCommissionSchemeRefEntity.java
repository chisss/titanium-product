package com.titanium.product.infrastructure.pricing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 定价包锁定的 Channel 佣金方案版本证据。
 */
@Entity
@Table(name = "t_product_pricing_plan_commission_ref")
@Getter
@Setter
public class PricingPlanCommissionSchemeRefEntity {

    @Id
    @Column(name = "ref_id", nullable = false, length = 36)
    private String refId;

    @Column(name = "plan_id", nullable = false, length = 36)
    private String planId;

    @Column(name = "channel_id", nullable = false, length = 64)
    private String channelId;

    @Column(name = "scheme_code", nullable = false, length = 64)
    private String schemeCode;

    @Column(name = "scheme_version", nullable = false, length = 32)
    private String schemeVersion;

    @Column(name = "scheme_hash", nullable = false, length = 64)
    private String schemeHash;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
