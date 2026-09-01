package com.titanium.product.api.response.config;

import java.time.LocalDateTime;

import com.titanium.metadata.enums.product.ProductEnum;

/**
 * 审核信息响应（api 契约，镜像领域值对象 AuditInfo）。
 *
 * @param auditorId 审核人ID
 * @param auditorName 审核人姓名
 * @param auditOpinion 审核意见
 * @param auditTime 审核时间
 * @param auditResult 审核结果
 */
public record AuditInfoResponse(String auditorId, String auditorName, String auditOpinion, LocalDateTime auditTime,
                                ProductEnum.AuditResult auditResult) {
}
