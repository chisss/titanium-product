package com.titanium.product.api.request;

import com.titanium.metadata.enums.product.ProductEnum;

import lombok.Data;

/**
 * 审核产品请求
 * 用于产品审核的请求参数
 */
@Data
public class AuditProductRequest {
    /** 审核结果（PASS/REJECT/RETURN） */
    private ProductEnum.AuditResult auditResult;
    /** 审核备注/意见 */
    private String auditOpinion;
    /** 审核人ID */
    private String auditorId;
    /** 审核人姓名 */
    private String auditorName;
}
