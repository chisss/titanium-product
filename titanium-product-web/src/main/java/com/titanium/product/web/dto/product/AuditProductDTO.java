package com.titanium.product.web.dto.product;

import com.titanium.metadata.enums.product.ProductEnum;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 审核产品请求（后台/端上 HTTP 入参）
 * <p>
 * 面向管理后台/端上的产品审核/驳回请求，由 {@code ProductWebMapper} 翻译为对应领域命令。
 * </p>
 */
@Data
public class AuditProductDTO {

    /** 审核结果（PASS/REJECT/RETURN） */
    private ProductEnum.AuditResult auditResult;
    /** 审核备注/意见 */
    @Size(max = 255)
    private String auditOpinion;
    /** 审核人ID */
    @Size(max = 64)
    private String auditorId;
    /** 审核人姓名 */
    @Size(max = 128)
    private String auditorName;
}
