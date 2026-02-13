package com.titanium.product.api.dto;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 产品DTO
 * 用于产品数据的传输，包含完整的产品配置信息
 */
@Data
public class ProductDTO {
    private String productId;
    private String productCode;
    private String productName;
    private String productDesc;
    private String form;
    private String insuranceType;
    private String category;
    private String version;
    private String status;
    private String originalProductId;
    private LocalDateTime effectiveTime;
    private LocalDateTime invalidTime;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;

    /** 投保条件（JSON对象或结构化DTO） */
    private Object insureCondition;
    /** 保障期间配置 */
    private Object coveragePeriod;
    /** 缴费方式配置 */
    private Object paymentConfig;
    /** 定价基础规则 */
    private Object pricingBasicRule;
    /** 出单流程配置 */
    private Object issuanceProcessConfig;
    /** 保单形态配置 */
    private Object policyFormConfig;
    /** 核保配置 */
    private Object underwritingConfig;
    /** 审核信息 */
    private Object auditInfo;

    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
