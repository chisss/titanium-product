package com.titanium.product.web.dto.product;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 更新产品条款关联请求（后台/端上 HTTP 入参）
 *
 * <p><b>整体替换</b>语义：提交的清单即替换后的全部绑定，不是增量。此前该能力只在应用层
 * {@code ProductCommandAppService.updateProductClauseRel} 存在而<b>零生产入口</b>（无端点、无 Provider、
 * 前端零引用），本条把入口补齐（web 侧）。</p>
 *
 * <p>🔴 空清单<b>不是</b>「解绑全部」而是非法输入：绑定非空是产品聚合的硬不变量，
 * 故 {@code @NotEmpty} 在此处即拒。仅 {@code DRAFT} 状态产品可改绑定，该状态前置由<b>聚合根</b>裁决
 * （web 层不复制该判断，见 {@code InsuranceProduct#handle(UpdateProductClauseRelCommand)}）。</p>
 */
@Data
public class UpdateProductClauseRelDTO {

    /**
     * 替换后的全部条款关联
     * <p>
     * 须含且仅含一条主条款、条款主键不得重复——两项均为聚合内的纯领域不变量，此处不重复表达。
     * </p>
     */
    @NotEmpty
    @Valid
    private List<ClauseRelDTO> clauseRels;
}
