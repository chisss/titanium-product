package com.titanium.product.infrastructure.pricing.entity.calculation;

import java.math.BigDecimal;

import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.product.common.enums.TaxPriceMode;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Product 确认计算结构化费用明细持久化实体。
 */
@Entity
@Table(name = "t_product_calculation_line")
@Getter
@Setter
public class CalculationLineDO {

    @EmbeddedId
    private CalculationLineId id;

    @Column(name = "component_code", nullable = false, length = 64)
    private String componentCode;

    @Column(name = "component_version", nullable = false, length = 32)
    private String componentVersion;

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

    @Column(name = "accounting_class", nullable = false, length = 64)
    private String accountingClass;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "base_amount", precision = 20, scale = 8)
    private BigDecimal baseAmount;

    @Column(name = "rate_value", precision = 20, scale = 8)
    private BigDecimal rate;

    @Column(name = "calculated_amount", nullable = false, precision = 20, scale = 8)
    private BigDecimal calculatedAmount;

    @Column(name = "node_code", nullable = false, length = 64)
    private String nodeCode;

    @Column(name = "customer_visible", nullable = false)
    private boolean customerVisible;

    @Column(name = "affects_customer_payable", nullable = false)
    private boolean affectsCustomerPayable;

    @Column(name = "jurisdiction_code", length = 64)
    private String jurisdictionCode;

    @Column(name = "regulatory_reference_id", length = 128)
    private String regulatoryReferenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_price_mode", length = 16)
    private TaxPriceMode taxPriceMode;

    @Column(name = "tax_policy_hash", length = 64)
    private String taxPolicyHash;

    @Column(name = "tax_exempt")
    private Boolean taxExempt;

    @Column(name = "commission_channel_id", length = 64)
    private String commissionChannelId;

    @Column(name = "commission_scheme_code", length = 64)
    private String commissionSchemeCode;

    @Column(name = "commission_scheme_version", length = 32)
    private String commissionSchemeVersion;

    @Column(name = "commission_scheme_hash", length = 64)
    private String commissionSchemeHash;

    @Column(name = "commission_beneficiary_type", length = 32)
    private String commissionBeneficiaryType;

    @Column(name = "commission_beneficiary_id", length = 64)
    private String commissionBeneficiaryId;

    @Column(name = "commission_split_rate", precision = 20, scale = 8)
    private BigDecimal commissionSplitRate;

    @Column(name = "commission_gross_amount", precision = 20, scale = 8)
    private BigDecimal commissionGrossAmount;

    @Column(name = "commission_installment_count")
    private Integer commissionInstallmentCount;

    @Column(name = "commission_clawback_months")
    private Integer commissionClawbackMonths;

    @Column(name = "description", length = 255)
    private String description;
}
