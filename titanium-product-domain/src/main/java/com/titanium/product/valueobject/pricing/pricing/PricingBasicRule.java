package com.titanium.product.valueobject.pricing.pricing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 定价基础规则值对象 表示产品的定价基础配置，包含定价类型、基础费率、定价因子等
 * <p>
 * 🔴 <b>{@code factors} 已废弃（m16-1904）</b>：产品级 factors 系数不参与保费试算（全域无消费方），
 * 非空值会被聚合 G7 守卫以 {@code PRICING_FACTORS_DEPRECATED} 明确拒绝，并提示改用定价计划的动态因子。
 * 保留该组件而非直接删除，是为了让误用者在<b>领域入口</b>也收到可执行的错误提示，而不是被静默忽略
 * （与 web 侧 {@code PricingBasicRuleDTO.factors} 口径一致）。
 * ⚠️ 删除须评估存量事件反序列化兼容性——本组件已固化在 {@code ProductCreatedEvent} /
 * {@code ProductRevisedEvent} 的载荷中，非尾部删除会令存量事件回放错位。
 * </p>
 *
 * @param pricingType 定价类型（固定费率/阶梯费率/因子定价）
 * @param baseRate 基础费率（如车险基础费率0.02）
 * @param factors 定价因子列表（如年龄因子、保额因子、车型因子）——🔴 <b>已废弃</b>，见类注释；非空即被 G7 守卫拒绝
 * @param rateFormula 费率计算公式（如 保费=保额×基础费率×∏定价因子）
 * @param typeSpecificConfig 险种专属配置（如投连险投资账户费率）
 */
public record PricingBasicRule(ProductEnum.PricingType pricingType, BigDecimal baseRate,
                               @Deprecated List<PricingFactor> factors,
                               String rateFormula, Map<String, Object> typeSpecificConfig) {
}
