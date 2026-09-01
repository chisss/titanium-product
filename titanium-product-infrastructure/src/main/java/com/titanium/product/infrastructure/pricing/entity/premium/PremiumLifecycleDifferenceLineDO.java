package com.titanium.product.infrastructure.pricing.entity.premium;

import java.math.BigDecimal;

import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.enums.pricing.ChargePayerType;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Product 生命周期单项费用差额实体。
 */
@Entity
@Table(name = "t_product_premium_lifecycle_adjustment_line")
@Getter
@Setter
public class PremiumLifecycleDifferenceLineDO {

    @EmbeddedId
    private PremiumLifecycleDifferenceLineId id;
    @Column(name = "component_code", nullable = false, length = 64)
    private String componentCode;
    @Column(name = "original_component_version", length = 32)
    private String originalComponentVersion;
    @Column(name = "replacement_component_version", length = 32)
    private String replacementComponentVersion;
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private ChargeCategory category;
    @Enumerated(EnumType.STRING)
    @Column(name = "amount_channel", nullable = false, length = 24)
    private AmountChannel amountChannel;
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 12)
    private ChargeDirection direction;
    @Enumerated(EnumType.STRING)
    @Column(name = "payer_type", nullable = false, length = 24)
    private ChargePayerType payerType;
    @Column(name = "accounting_class", nullable = false, length = 64)
    private String accountingClass;
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    @Enumerated(EnumType.STRING)
    @Column(name = "original_direction", length = 12)
    private ChargeDirection originalDirection;
    @Column(name = "before_amount", nullable = false, precision = 20, scale = 8)
    private BigDecimal beforeAmount;
    @Enumerated(EnumType.STRING)
    @Column(name = "replacement_direction", length = 12)
    private ChargeDirection replacementDirection;
    @Column(name = "after_amount", nullable = false, precision = 20, scale = 8)
    private BigDecimal afterAmount;
    @Column(name = "difference_amount", nullable = false, precision = 20, scale = 8)
    private BigDecimal differenceAmount;
    @Column(name = "customer_visible", nullable = false)
    private boolean customerVisible;
    @Column(name = "affects_customer_payable", nullable = false)
    private boolean affectsCustomerPayable;
    @Column(name = "description", length = 500)
    private String description;
}
