package com.titanium.product.api.request;

import lombok.Data;

/**
 * 审核产品请求
 * 用于产品审核的请求参数
 */
@Data
public class AuditProductRequest {
    /**
     * 产品ID
     */
    private String productId;
    
    /**
     * 审核结果（通过/拒绝）
     */
    private boolean approved;
    
    /**
     * 审核备注
     */
    private String auditRemark;
    
    /**
     * 审核人
     */
    private String auditedBy;
}