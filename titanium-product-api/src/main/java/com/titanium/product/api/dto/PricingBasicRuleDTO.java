package com.titanium.product.api.dto;

import lombok.Data;

/**
 * 定价基础规则DTO
 * 用于表示产品的定价基础规则
 */
@Data
public class PricingBasicRuleDTO {
    /**
     * 定价类型（FIXED-固定定价/AGE-年龄定价/OCCUPATION-职业定价/COMBINED-组合定价）
     */
    private String pricingType;
    
    /**
     * 定价系数（JSON格式）
     */
    private String pricingFactors;
    
    /**
     * 最低保费
     */
    private Double minPremium;
    
    /**
     * 最高保费
     */
    private Double maxPremium;
    
    /**
     * 费率表ID
     */
    private String rateTableId;
}