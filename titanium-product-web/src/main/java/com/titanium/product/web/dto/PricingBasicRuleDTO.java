package com.titanium.product.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.valueobject.pricing.pricing.PricingFactor;

import lombok.Data;

/**
 * 定价基础规则请求（后台/端上 HTTP 入参）
 * <p>
 * 面向管理后台/端上的产品定价基础规则请求，由 {@code ProductWebMapper} 翻译为领域值对象
 * {@code PricingBasicRule}。web 层可依赖 domain 值对象/命令，故此处直接承载 {@code PricingFactor}。
 * </p>
 */
@Data
public class PricingBasicRuleDTO {

    /** 定价类型（FIXED/AGE/OCCUPATION/COMBINED） */
    private ProductEnum.PricingType pricingType;
    /** 基础费率 */
    private BigDecimal baseRate;
    /** 定价因子列表 */
    private List<PricingFactor> factors;
    /** 费率计算公式 */
    private String rateFormula;
    /** 定价系数（JSON格式） */
    private String pricingFactors;
    /** 最低保费 */
    private Double minPremium;
    /** 最高保费 */
    private Double maxPremium;
    /** 费率表ID */
    private String rateTableId;
    /** 险种专属配置 */
    private Map<String, Object> typeSpecificConfig;
}
