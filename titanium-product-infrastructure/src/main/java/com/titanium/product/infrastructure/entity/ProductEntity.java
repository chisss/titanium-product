package com.titanium.product.infrastructure.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;

import lombok.Getter;
import lombok.Setter;

/**
 * 产品数据库实体
 * 映射产品表，包含产品的基本信息、状态、版本及各种配置JSON字段
 */
@Entity
@Table(name = "t_product", indexes = {
        @Index(name = "idx_product_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_product_status", columnList = "status"),
        @Index(name = "idx_product_insurance_type", columnList = "insurance_type"),
        @Index(name = "idx_product_original_id", columnList = "original_product_id"),
        @Index(name = "idx_product_type_status", columnList = "insurance_type, status"),
        @Index(name = "uk_product_code_tenant", columnList = "product_code, tenant_id", unique = true)
})
@Getter
@Setter
public class ProductEntity {

    @Id
    @Column(name = "product_id", length = 36, nullable = false)
    private String productId;

    @Column(name = "product_code", length = 50, nullable = false)
    private String productCode;

    @Column(name = "product_name", length = 100, nullable = false)
    private String productName;

    @Column(name = "product_desc", columnDefinition = "TEXT")
    private String productDesc;

    @Enumerated(EnumType.STRING)
    @Column(name = "form", length = 20, nullable = false)
    private ProductEnum.ProductForm form;

    @Enumerated(EnumType.STRING)
    @Column(name = "insurance_type", length = 20, nullable = false)
    private InsuranceType insuranceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20, nullable = false)
    private ProductEnum.ProductCategory category;

    @Column(name = "version", length = 10, nullable = false)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ProductEnum.ProductStatus status;

    @Column(name = "original_product_id", length = 36)
    private String originalProductId;

    @Column(name = "effective_time")
    private LocalDateTime effectiveTime;

    @Column(name = "invalid_time")
    private LocalDateTime invalidTime;

    @Column(name = "sale_start_time")
    private LocalDateTime saleStartTime;

    @Column(name = "sale_end_time")
    private LocalDateTime saleEndTime;

    // ====== JSON值对象字段 ======

    @Column(name = "insure_condition", columnDefinition = "JSON")
    private String insureCondition;

    @Column(name = "coverage_period", columnDefinition = "JSON")
    private String coveragePeriod;

    @Column(name = "payment_config", columnDefinition = "JSON")
    private String paymentConfig;

    @Column(name = "pricing_basic_rule", columnDefinition = "JSON")
    private String pricingBasicRule;

    @Column(name = "issuance_process_config", columnDefinition = "JSON")
    private String issuanceProcessConfig;

    @Column(name = "policy_form_config", columnDefinition = "JSON")
    private String policyFormConfig;

    @Column(name = "underwriting_config", columnDefinition = "JSON")
    private String underwritingConfig;

    @Column(name = "sales_channels", columnDefinition = "JSON")
    private String salesChannels;

    @Column(name = "attach_product_ids", columnDefinition = "JSON")
    private String attachProductIds;

    // ====== 审核信息 ======

    @Column(name = "auditor_id", length = 50)
    private String auditorId;

    @Column(name = "auditor_name", length = 50)
    private String auditorName;

    @Column(name = "audit_opinion", length = 500)
    private String auditOpinion;

    @Column(name = "audit_time")
    private LocalDateTime auditTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_result", length = 20)
    private ProductEnum.AuditResult auditResult;

    // ====== 规则引擎预留 ======

    @Column(name = "pricing_rule_set_id", length = 50)
    private String pricingRuleSetId;

    @Column(name = "insure_condition_rule_set_id", length = 50)
    private String insureConditionRuleSetId;

    @Column(name = "underwriting_rule_set_id", length = 50)
    private String underwritingRuleSetId;

    // ====== 租户与审计 ======

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 50, nullable = false)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 50, nullable = false)
    private String updatedBy;
}
