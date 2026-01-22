package com.titanium.product.api.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产品DTO
 * 用于产品数据的传输
 */
@Data
public class ProductDTO {
    /**
     * 产品ID
     */
    private String productId;
    
    /**
     * 产品名称
     */
    private String productName;
    
    /**
     * 产品形态（INDIVIDUAL-个险/GROUP-团险）
     */
    private String form;
    
    /**
     * 险种类型（CAR-车险/LIFE-寿险/ACCIDENT-意外险/PET-宠物险/PROPERTY-财产险/INVESTMENT-投连险）
     */
    private String insuranceType;
    
    /**
     * 产品版本（如 V1.0/V2.0）
     */
    private String version;
    
    /**
     * 产品状态（DRAFT-草稿/AUDITING-审核中/EFFECTIVE-生效/INVALID-下架）
     */
    private String status;
    
    /**
     * 生效时间
     */
    private LocalDateTime effectiveTime;
    
    /**
     * 下架时间
     */
    private LocalDateTime invalidTime;
    
    /**
     * 投保条件
     */
    private InsureConditionDTO insureCondition;
    
    /**
     * 定价基础规则
     */
    private PricingBasicRuleDTO pricingBasicRule;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 创建人
     */
    private String createdBy;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 更新人
     */
    private String updatedBy;
}