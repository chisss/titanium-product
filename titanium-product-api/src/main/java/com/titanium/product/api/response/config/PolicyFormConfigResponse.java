package com.titanium.product.api.response.config;

import java.util.List;

import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 保单形态配置响应（api 契约，镜像领域值对象 PolicyFormConfig）。
 * <p>Policy 域据此决定生成保单的结构形态（个单/团单清单制/家庭单/联合保单）。</p>
 *
 * @param policyFormType 保单形态类型
 * @param listPolicySupported 是否支持清单制
 * @param minSubPolicyCount 清单制子保单最小数量
 * @param maxSubPolicyCount 清单制子保单最大数量
 * @param subPolicyIndependentEndorsement 子保单是否可独立退保/保全
 * @param supportedLevels 支持的保单层级列表
 * @param beneficiaryRequired 是否需要指定受益人
 * @param maxBeneficiaryCount 受益人最大人数
 */
public record PolicyFormConfigResponse(ProductEnum.PolicyFormType policyFormType, boolean listPolicySupported,
                                       Integer minSubPolicyCount, Integer maxSubPolicyCount,
                                       boolean subPolicyIndependentEndorsement,
                                       List<ProductEnum.PolicyLevel> supportedLevels, boolean beneficiaryRequired,
                                       Integer maxBeneficiaryCount) {
}
