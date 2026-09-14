package com.titanium.product.web.dto.pricing.basicrule;

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
 * <p>
 * 🔴 <b>字段集与领域值对象对齐</b>：DTO 字段名必须全部存在于 {@link PricingBasicRule}，否则 MapStruct
 * 找不到目标属性会<b>静默丢弃</b>该入参（既不报错也不落库），运营以为配置生效实则无效。历史上
 * {@code pricingFactors}/{@code minPremium}/{@code maxPremium}/{@code rateTableId} 四字段即属此列，
 * 已清除。该约束由 {@code PricingBasicRuleDTOContractTest} 反射断言固化，新增字段前先扩展领域值对象。
 * </p>
 *
 * @see com.titanium.product.valueobject.pricing.pricing.PricingBasicRule
 */
@Data
public class PricingBasicRuleDTO {

    /** 定价类型（FIXED/AGE/OCCUPATION/COMBINED） */
    private ProductEnum.PricingType pricingType;
    /** 基础费率 */
    private BigDecimal baseRate;
    /**
     * 定价因子列表
     *
     * @deprecated 产品级 factors 系数不参与保费试算（无消费方），仅作占位保留：非空值会被聚合的
     *         G7 守卫以 {@code PRICING_FACTORS_DEPRECATED} 明确拒绝，并提示改用定价计划的动态因子。
     *         保留该入参而非直接删除，是为了让误用者收到可执行的错误提示，而不是被静默忽略。
     */
    @Deprecated
    private List<PricingFactor> factors;
    /** 费率计算公式 */
    private String rateFormula;
    /** 险种专属配置 */
    private Map<String, Object> typeSpecificConfig;
}
