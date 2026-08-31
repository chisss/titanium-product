package com.titanium.product.infrastructure.pricing.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.common.enums.DynamicFactorMissingPolicy;
import com.titanium.product.common.enums.DynamicFactorSourceType;
import com.titanium.product.common.enums.DynamicFactorTransformType;
import com.titanium.product.common.enums.DynamicFactorValueTimePolicy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** 动态因子持久化实体。 */
@Entity
@Table(name = "t_product_dynamic_factor")
@Getter
@Setter
public class DynamicFactorDO {

    @Id
    @Column(name = "factor_id", nullable = false, length = 36)
    private String factorId;
    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;
    @Column(name = "factor_code", nullable = false, length = 64)
    private String factorCode;
    @Column(name = "factor_version", nullable = false, length = 32)
    private String factorVersion;
    @Column(name = "factor_name", nullable = false, length = 128)
    private String factorName;
    @Column(name = "description", nullable = false, length = 500)
    private String description;
    @Column(name = "feature_code", nullable = false, length = 64)
    private String featureCode;
    @Column(name = "feature_definition_version", nullable = false, length = 128)
    private String featureDefinitionVersion;
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private DynamicFactorSourceType sourceType;
    @Enumerated(EnumType.STRING)
    @Column(name = "value_time_policy", nullable = false, length = 32)
    private DynamicFactorValueTimePolicy valueTimePolicy;
    @Column(name = "lower_bound", precision = 20, scale = 8)
    private BigDecimal lowerBound;
    @Column(name = "upper_bound", precision = 20, scale = 8)
    private BigDecimal upperBound;
    @Enumerated(EnumType.STRING)
    @Column(name = "missing_policy", nullable = false, length = 24)
    private DynamicFactorMissingPolicy missingPolicy;
    @Column(name = "default_value", precision = 20, scale = 8)
    private BigDecimal defaultValue;
    @Enumerated(EnumType.STRING)
    @Column(name = "transform_type", nullable = false, length = 24)
    private DynamicFactorTransformType transformType;
    @Column(name = "multiplier", nullable = false, precision = 20, scale = 8)
    private BigDecimal multiplier;
    @Column(name = "offset_value", nullable = false, precision = 20, scale = 8)
    private BigDecimal offset;
    @Column(name = "replayable", nullable = false)
    private boolean replayable;
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
