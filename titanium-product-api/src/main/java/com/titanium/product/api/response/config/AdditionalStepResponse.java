package com.titanium.product.api.response.config;

import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 附加业务步骤响应（api 契约，镜像领域值对象 AdditionalStep）。
 * <p>出单流程中险种专属的附加步骤，如验车/体检/现场勘验/人员清单上传。</p>
 *
 * @param stepType 步骤标识
 * @param stepName 步骤名称
 * @param executeBeforeStep 执行顺序（在哪个标准步骤之前执行）
 * @param mandatory 是否必须
 * @param description 步骤描述/说明
 * @param externalSystemCode 对接的外部系统标识
 */
public record AdditionalStepResponse(ProductEnum.AdditionalStepType stepType, String stepName,
                                     ProductEnum.IssuanceStep executeBeforeStep, boolean mandatory, String description,
                                     String externalSystemCode) {
}
