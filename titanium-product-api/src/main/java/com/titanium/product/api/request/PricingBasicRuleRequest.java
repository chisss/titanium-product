package com.titanium.product.api.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.domain.valueobject.PricingFactor;

import lombok.Data;

/**
 * 定价基础规则请求 用于产品定价基础规则的请求参数
 */
@Data
public class PricingBasicRuleRequest {
    /**
     * 定价类型（FIXED-固定定价/AGE-年龄定价/OCCUPATION-职业定价/COMBINED-组合定价）
     */
    private ProductEnum.PricingType pricingType;

    private BigDecimal              baseRate;

    private List<PricingFactor>     factors;

    private String                  rateFormula;

    /**
     * 定价系数（JSON格式）
     */
    private String                  pricingFactors;

    /**
     * 最低保费
     */
    private Double                  minPremium;

    /**
     * 最高保费
     */
    private Double                  maxPremium;

    /**
     * 费率表ID
     */
    private String                  rateTableId;

    private Map<String, Object>     typeSpecificConfig;
}
