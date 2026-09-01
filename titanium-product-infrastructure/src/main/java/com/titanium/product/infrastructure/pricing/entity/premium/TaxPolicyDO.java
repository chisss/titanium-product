package com.titanium.product.infrastructure.pricing.entity.premium;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.common.enums.TaxPriceMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 税费策略持久化主表。
 */
@Entity
@Table(name = "t_product_tax_policy")
@Getter
@Setter
public class TaxPolicyDO {

    @Id
    @Column(name = "policy_id", nullable = false, length = 36)
    private String policyId;
    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;
    @Column(name = "policy_code", nullable = false, length = 64)
    private String policyCode;
    @Column(name = "policy_version", nullable = false, length = 32)
    private String policyVersion;
    @Column(name = "policy_name", nullable = false, length = 128)
    private String policyName;
    @Column(name = "description", length = 500)
    private String description;
    @Column(name = "jurisdiction_code", nullable = false, length = 64)
    private String jurisdictionCode;
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private ChargeCategory category;
    @Enumerated(EnumType.STRING)
    @Column(name = "payer_type", nullable = false, length = 32)
    private ChargePayerType payerType;
    @Enumerated(EnumType.STRING)
    @Column(name = "price_mode", nullable = false, length = 16)
    private TaxPriceMode priceMode;
    @Column(name = "tax_rate", nullable = false, precision = 20, scale = 8)
    private BigDecimal taxRate;
    @Column(name = "accounting_class", nullable = false, length = 64)
    private String accountingClass;
    @Column(name = "regulatory_reference_id", nullable = false, length = 128)
    private String regulatoryReferenceId;
    @Column(name = "exemption_feature_code", length = 64)
    private String exemptionFeatureCode;
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
