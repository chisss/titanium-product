package com.titanium.product.api.request.product;

import lombok.Data;

/**
 * 审核产品远程契约 DTO
 * <p>
 * 面向其它微服务的产品审核/驳回入参。审核结果由聚合根侧决定（通过/驳回走不同命令），
 * 本 DTO 仅承载审核人信息与审核意见，保持 api 层自包含（无领域枚举依赖）。
 * </p>
 */
@Data
public class AuditProductRequest {

    /** 审核意见/备注 */
    private String auditOpinion;
    /** 审核人ID */
    private String auditorId;
    /** 审核人姓名 */
    private String auditorName;
}
