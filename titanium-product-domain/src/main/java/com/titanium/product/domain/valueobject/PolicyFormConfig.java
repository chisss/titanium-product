package com.titanium.product.domain.valueobject;

import java.util.List;

import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 保单形态配置值对象
 *
 * 定义该产品生成的保单结构形态：
 * - 个单：一个投保人 → 一张保单
 * - 团单/清单制：一个主保单 → N个子保单（按被保险人拆分）
 * - 家庭单：一个主保单 → M个家庭成员子保单
 * - 联合保单：多个投保人共同持有一张保单
 *
 * Policy域根据此配置决定生成保单时的结构。
 *
 * @param policyFormType                保单形态类型
 * @param listPolicySupported           是否支持清单制
 * @param minSubPolicyCount             清单制子保单最小数量
 * @param maxSubPolicyCount             清单制子保单最大数量
 * @param subPolicyIndependentEndorsement 子保单是否可独立退保/保全
 * @param supportedLevels               支持的保单层级列表
 * @param beneficiaryRequired           是否需要指定受益人
 * @param maxBeneficiaryCount           受益人最大人数
 */
public record PolicyFormConfig(
        ProductEnum.PolicyFormType policyFormType,
        boolean listPolicySupported,
        Integer minSubPolicyCount,
        Integer maxSubPolicyCount,
        boolean subPolicyIndependentEndorsement,
        List<ProductEnum.PolicyLevel> supportedLevels,
        boolean beneficiaryRequired,
        Integer maxBeneficiaryCount
) {
}
