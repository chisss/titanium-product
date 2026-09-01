package com.titanium.product.infrastructure.pricing.entity.pricing;

import java.time.LocalDateTime;

import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.product.common.enums.PricingPlanStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Product 定价方案持久化实体。
 */
@Entity
@Table(name = "t_product_pricing_plan")
@Getter
@Setter
public class PricingPlanDO {

    @Id
    @Column(name = "plan_id", nullable = false, length = 36)
    private String planId;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(name = "product_version", nullable = false, length = 32)
    private String productVersion;

    @Column(name = "plan_version", nullable = false, length = 32)
    private String planVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_mode", nullable = false, length = 32)
    private PricingMode pricingMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PricingPlanStatus status;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "rate_table_code", length = 64)
    private String rateTableCode;

    @Column(name = "rate_table_version", length = 32)
    private String rateTableVersion;

    @Column(name = "rate_dimension_keys_json", columnDefinition = "TEXT")
    private String rateDimensionKeysJson;

    @Column(name = "feature_contract_id", length = 64)
    private String featureContractId;

    @Column(name = "feature_contract_version", length = 32)
    private String featureContractVersion;

    @Column(name = "feature_requirements_json", nullable = false, columnDefinition = "TEXT")
    private String featureRequirementsJson;

    @Column(name = "artifact_code", length = 64)
    private String artifactCode;

    @Column(name = "artifact_version", length = 32)
    private String artifactVersion;

    @Column(name = "input_schema_version", length = 32)
    private String inputSchemaVersion;

    @Column(name = "artifact_hash", length = 64)
    private String artifactHash;

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

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "test_case_count", nullable = false)
    private int testCaseCount;

    @Column(name = "tenant_id", nullable = false, length = 32)
    private String tenantId;

    @Column(name = "create_time", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
