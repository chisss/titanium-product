package com.titanium.product.valueobject.pricing.pricing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 定价基础规则值对象 表示产品的定价基础配置，包含定价类型、基础费率、定价因子等
 *
 * @param pricingType 定价类型（固定费率/阶梯费率/因子定价）
 * @param baseRate 基础费率（如车险基础费率0.02）
 * @param factors 定价因子列表（如年龄因子、保额因子、车型因子）
 * @param rateFormula 费率计算公式（如 保费=保额×基础费率×∏定价因子）
 * @param typeSpecificConfig 险种专属配置（如投连险投资账户费率）
 */
public record PricingBasicRule(ProductEnum.PricingType pricingType, BigDecimal baseRate, List<PricingFactor> factors,
                               String rateFormula, Map<String, Object> typeSpecificConfig) {
}
