package com.titanium.product.infrastructure.pricing.entity;

import java.time.LocalDateTime;

import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.common.enums.ChargeCalculationSource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Product 费用项持久化实体。
 */
@Entity
@Table(name = "t_product_charge_component")
@Getter
@Setter
public class ChargeComponentEntity {

    @Id
    @Column(name = "component_id", nullable = false, length = 36)
    private String componentId;
    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;
    @Column(name = "component_code", nullable = false, length = 64)
    private String componentCode;
    @Column(name = "component_version", nullable = false, length = 32)
    private String componentVersion;
    @Column(name = "component_name", nullable = false, length = 128)
    private String componentName;
    @Column(name = "description", length = 500)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private ChargeCategory category;
    @Enumerated(EnumType.STRING)
    @Column(name = "amount_channel", nullable = false, length = 32)
    private AmountChannel amountChannel;
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 16)
    private ChargeDirection direction;
    @Enumerated(EnumType.STRING)
    @Column(name = "payer_type", nullable = false, length = 32)
    private ChargePayerType payerType;
    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_source", nullable = false, length = 32)
    private ChargeCalculationSource calculationSource;
    @Column(name = "accounting_class", nullable = false, length = 64)
    private String accountingClass;
    @Column(name = "customer_visible", nullable = false)
    private boolean customerVisible;
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
}
