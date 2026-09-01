package com.titanium.product.api.response.config;

import java.util.List;

import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 出单流程配置响应（api 契约，镜像领域值对象 IssuanceProcessConfig）。
 * <p>Policy 域据此动态编排出单步骤（一步/两步/三步/自定义）。</p>
 *
 * @param issuanceMode 出单模式（ONE_STEP/TWO_STEP/THREE_STEP/CUSTOM）
 * @param steps 出单步骤链（有序列表）
 * @param proposalRequired 是否需要意向单环节
 * @param underwritingSkippable 是否支持跳过核保
 * @param prepaymentRequired 是否需要前置缴费
 * @param issuanceDeadlineDays 出单有效期（天）
 * @param additionalSteps 附加业务步骤列表
 */
public record IssuanceProcessConfigResponse(ProductEnum.IssuanceMode issuanceMode,
                                            List<ProductEnum.IssuanceStep> steps, boolean proposalRequired,
                                            boolean underwritingSkippable, boolean prepaymentRequired,
                                            Integer issuanceDeadlineDays,
                                            List<AdditionalStepResponse> additionalSteps) {
}
