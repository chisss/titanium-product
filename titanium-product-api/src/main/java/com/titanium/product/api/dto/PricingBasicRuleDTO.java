package com.titanium.product.api.dto;

import java.math.BigDecimal;

import com.titanium.metadata.enums.product.ProductEnum;

import lombok.Data;

/**
 * 定价基础规则DTO
 * 用于表示产品的定价基础规则
 */
@Data
public class PricingBasicRuleDTO {
    /**
     * 定价类型（FIXED-固定费率/STEP-阶梯费率/FACTOR-因子定价）
     */
    private ProductEnum.PricingType pricingType;

    /**
     * 基础费率（如车险基础费率 0.02），供保费计算使用
     */
    private BigDecimal baseRate;

    /**
     * 费率计算公式（如 保费=保额×基础费率×∏定价因子）
     */
    private String rateFormula;

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
