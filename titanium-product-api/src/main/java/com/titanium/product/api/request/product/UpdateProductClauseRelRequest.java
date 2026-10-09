package com.titanium.product.api.request.product;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 更新产品条款关联远程契约（上游 → product）
 *
 * <p><b>整体替换</b>语义：提交的清单即替换后的全部绑定。供其它微服务在需要程序化改写产品-条款绑定时调用
 * （后台/端上的同名能力走 web 层 {@code PUT /web/v1/products/{productId}/clauses}，两条入口平行收敛到
 * 同一个 application 薄门面，互不依赖）。</p>
 *
 * <p>api 层自包含：条款关联在此以 {@link ClauseRel} 承载，不复用 web 层类型（项目 CLAUDE.md §3.4.11 ①）。</p>
 */
@Data
public class UpdateProductClauseRelRequest {

    /** 替换后的全部条款关联（不得为空；绑定非空是产品聚合的硬不变量） */
    @NotEmpty
    @Valid
    private List<ClauseRel> clauseRels;

    /**
     * 条款关联条目
     *
     * @param clauseId      条款<b>主键</b>（非条款编码）
     * @param clauseVersion 绑定的条款版本
     * @param isMainClause  是否主条款；{@code null} 视同非主条款
     */
    public record ClauseRel(String clauseId, String clauseVersion, Boolean isMainClause) {
    }
}
