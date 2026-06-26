package com.titanium.product.infrastructure.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.titanium.metadata.enums.CommonStatus;
import com.titanium.metadata.enums.InsuranceType;
import com.titanium.metadata.enums.product.ProductEnum;

import lombok.Getter;
import lombok.Setter;

/**
 * 产品模板数据库实体
 * 复杂值对象使用 JSON 字符串存储
 */
@Entity
@Table(name = "t_product_template")
@Getter
@Setter
public class ProductTemplateDO {

    @Id
    @Column(name = "template_id", length = 36, nullable = false)
    private String templateId;

    @Column(name = "template_code", length = 64, nullable = false, unique = true)
    private String templateCode;

    @Column(name = "template_name", length = 100, nullable = false)
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "insurance_category", length = 20)
    private ProductEnum.ProductCategory insuranceCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "insurance_type", length = 30, nullable = false)
    private InsuranceType insuranceType;

    @Column(name = "product_id", length = 36)
    private String productId;

    @Column(name = "issuance_mode", length = 20, nullable = false)
    private String issuanceMode;

    @Column(name = "policy_stages_json", columnDefinition = "TEXT")
    private String policyStagesJson;

    @Column(name = "underwriting_config_json", columnDefinition = "TEXT")
    private String underwritingConfigJson;

    @Column(name = "policy_structure_json", columnDefinition = "TEXT")
    private String policyStructureJson;

    @Column(name = "maintenance_config_json", columnDefinition = "TEXT")
    private String maintenanceConfigJson;

    @Column(name = "claim_config_json", columnDefinition = "TEXT")
    private String claimConfigJson;

    @Column(name = "billing_config_json", columnDefinition = "TEXT")
    private String billingConfigJson;

    @Column(name = "reinsurance_config_json", columnDefinition = "TEXT")
    private String reinsuranceConfigJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private CommonStatus status;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
