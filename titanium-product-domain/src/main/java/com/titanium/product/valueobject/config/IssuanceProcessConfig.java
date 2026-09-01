package com.titanium.product.valueobject.config;

import java.util.List;

import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.product.valueobject.AdditionalStep;

/**
 * 出单流程配置值对象 核心设计思想： - 当前Policy域硬编码了 Proposal→Insurance→Policy 三步出单 - 但实际业务中： ·
 * 车险（交强险）= 一步出单（录入即出单，无需核保） · 短期意外险/旅行险 = 两步出单（投保→保单，自动核保通过） · 寿险/重疾险 =
 * 三步出单（意向单→投保单→核保→保单） · 团体寿险 = 三步出单 + 清单制（主保单+N个子保单） · 高保额财产险 = 三步 +
 * 线下勘验（意向→勘验→投保→核保→保单） Product域通过此值对象定义该产品的出单步骤链路，Policy域消费此配置动态编排流程。
 *
 * @param issuanceMode 出单模式（ONE_STEP/TWO_STEP/THREE_STEP/CUSTOM）
 * @param steps 出单步骤链（有序列表）
 * @param proposalRequired 是否需要意向单环节
 * @param underwritingSkippable 是否支持跳过核保
 * @param prepaymentRequired 是否需要前置缴费
 * @param issuanceDeadlineDays 出单有效期（天）
 * @param additionalSteps 附加业务步骤列表
 */
public record IssuanceProcessConfig(ProductEnum.IssuanceMode issuanceMode, List<ProductEnum.IssuanceStep> steps,
                                    boolean proposalRequired, boolean underwritingSkippable, boolean prepaymentRequired,
                                    Integer issuanceDeadlineDays, List<AdditionalStep> additionalSteps) {
}
