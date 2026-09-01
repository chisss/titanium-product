package com.titanium.product.infrastructure.pricing.entity.premium;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.common.enums.PricingCalculationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Product 确认计算持久化实体。
 */
@Entity
@Table(name = "t_product_premium_calculation")
@Getter
@Setter
public class PremiumCalculationDO {

    @Id
    @Column(name = "calculation_id", nullable = false, length = 36)
    private String calculationId;
    @Column(name = "calculation_request_id", nullable = false, length = 64)
    private String calculationRequestId;
    @Column(name = "biz_no", nullable = false, length = 64)
    private String bizNo;
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 32)
    private PricingCalculationPurpose purpose;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PricingCalculationStatus status;
    @Column(name = "tenant_id", nullable = false, length = 32)
    private String tenantId;
    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;
    @Column(name = "product_version", nullable = false, length = 32)
    private String productVersion;
    @Column(name = "business_time", nullable = false)
    private LocalDateTime businessTime;
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    @Column(name = "standard_premium", nullable = false, precision = 20, scale = 8)
    private BigDecimal standardPremium;
    @Column(name = "total_premium", nullable = false, precision = 20, scale = 8)
    private BigDecimal totalPremium;
    @Column(name = "installment_amount", nullable = false, precision = 20, scale = 8)
    private BigDecimal installmentAmount;
    @Column(name = "periods", nullable = false)
    private int periods;
    @Column(name = "adjustments_json", nullable = false, columnDefinition = "LONGTEXT")
    private String adjustmentsJson;
    @Column(name = "request_snapshot_json", nullable = false, columnDefinition = "LONGTEXT")
    private String requestSnapshotJson;
    @Column(name = "pricing_plan_version", length = 32)
    private String pricingPlanVersion;
    @Column(name = "pricing_plan_content_hash", length = 64)
    private String pricingPlanContentHash;
    @Column(name = "rate_table_code", length = 64)
    private String rateTableCode;
    @Column(name = "rate_table_version", length = 32)
    private String rateTableVersion;
    @Column(name = "rate_table_content_hash", length = 64)
    private String rateTableContentHash;
    @Column(name = "feature_snapshot_id", length = 64)
    private String featureSnapshotId;
    @Column(name = "dynamic_factor_evidence_json", columnDefinition = "LONGTEXT")
    private String dynamicFactorEvidenceJson;
    @Column(name = "rule_artifact_code", length = 64)
    private String ruleArtifactCode;
    @Column(name = "rule_artifact_version", length = 32)
    private String ruleArtifactVersion;
    @Column(name = "rule_artifact_hash", length = 64)
    private String ruleArtifactHash;
    @Column(name = "calculation_model_code", length = 64)
    private String calculationModelCode;
    @Column(name = "calculation_model_version", length = 32)
    private String calculationModelVersion;
    @Column(name = "calculation_model_hash", length = 64)
    private String calculationModelHash;
    @Column(name = "rounding_scale", nullable = false)
    private int roundingScale;
    @Column(name = "rounding_mode", nullable = false, length = 32)
    private String roundingMode;
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;
    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;
    @Column(name = "result_hash", nullable = false, length = 64)
    private String resultHash;
    @Column(name = "create_time", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createTime;
}
