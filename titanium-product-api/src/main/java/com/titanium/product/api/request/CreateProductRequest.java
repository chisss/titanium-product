package com.titanium.product.api.request;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 创建产品请求
 * 用于创建产品的请求参数
 */
@Data
public class CreateProductRequest {
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
     * 投保条件
     */
    private InsureConditionRequest insureCondition;
    
    /**
     * 绑定的条款ID列表
     */
    private List<String> clauseIds;
    
    /**
     * 条款版本映射
     */
    private Map<String, String> clauseVersionMap;
    
    /**
     * 主条款ID
     */
    private String mainClauseId;
    
    /**
     * 定价基础规则
     */
    private PricingBasicRuleRequest pricingBasicRule;
    
    /**
     * 创建人
     */
    private String createdBy;
}