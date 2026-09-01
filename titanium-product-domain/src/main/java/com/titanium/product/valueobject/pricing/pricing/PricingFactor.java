package com.titanium.product.valueobject.pricing.pricing;

import java.math.BigDecimal;
import java.util.Map;


/**
 * 定价因子值对象 表示定价过程中的各种因子，如年龄因子、车型因子等
 *
 * @param factorName 因子名称
 * @param factorDesc 因子说明
 * @param factorValues 因子值映射（区间→因子）
 */
public record PricingFactor(String factorName, String factorDesc, Map<String, BigDecimal> factorValues) {
}
