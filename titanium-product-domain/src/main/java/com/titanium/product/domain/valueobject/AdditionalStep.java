package com.titanium.product.domain.valueobject;

import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 附加业务步骤值对象
 *
 * 不同险种在标准出单链路中可能插入的附加步骤
 * 示例：
 *   车险 → VEHICLE_INSPECTION（验车）
 *   寿险 → HEALTH_CHECK（体检）
 *   财产险 → FIELD_SURVEY（现场勘验）
 *   团险 → MEMBER_LIST_UPLOAD（人员清单上传）
 *
 * @param stepType           步骤标识
 * @param stepName           步骤名称
 * @param executeBeforeStep  执行顺序（在哪个标准步骤之前执行）
 * @param mandatory          是否必须
 * @param description        步骤描述/说明
 * @param externalSystemCode 对接的外部系统标识
 */
public record AdditionalStep(
        ProductEnum.AdditionalStepType stepType,
        String stepName,
        ProductEnum.IssuanceStep executeBeforeStep,
        boolean mandatory,
        String description,
        String externalSystemCode
) {
}
