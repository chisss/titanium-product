package com.titanium.product.domain.valueobject;

import java.time.LocalDateTime;

import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 审核信息值对象
 * 记录产品审核的相关信息
 *
 * @param auditorId    审核人ID
 * @param auditorName  审核人姓名
 * @param auditOpinion 审核意见
 * @param auditTime    审核时间
 * @param auditResult  审核结果
 */
public record AuditInfo(
        String auditorId,
        String auditorName,
        String auditOpinion,
        LocalDateTime auditTime,
        ProductEnum.AuditResult auditResult
) {
}
