package com.titanium.product.valueobject.config;

import java.util.List;
import java.util.Map;

/**
 * 产品条款绑定要素（创建产品命令的内聚要素块）。
 *
 * <p>三字段一体：绑定的条款主键列表、各条款的显式版本映射（可选元数据）、主条款主键。
 * 三者在创建链路上同进同出——应用层条款可绑定校验与聚合内 {@code ProductClauseRel} 装配
 * 都同时取用三者，收敛为值对象后 {@code CreateProductCommand} 少对齐两个位置参数
 * （DSN-04「大 record 拆分」样板，m25-06）。</p>
 *
 * <p>🔴 与 {@code com.titanium.product.port.clause.ClauseBindingCheck} 不是一回事：
 * 彼为「条款可绑定性核查**结论**」（出站端口的返回值），本为「绑定的**输入要素**」（命令入参块）。</p>
 *
 * <p>约束语义（与拆分前一致，由聚合 {@code validateCreateCommand} 强制）：
 * {@code clauseIds} 不得为空（null 或空列表都被拒，文案「产品必须绑定至少一条条款」）、
 * 不得重复；{@code mainClauseId} 必须包含在 {@code clauseIds} 内；
 * {@code clauseVersionMap} 为可选元数据，缺省视为无显式版本。</p>
 *
 * @param clauseIds        绑定的条款主键列表（聚合门禁要求非空且不重复）
 * @param clauseVersionMap 条款显式版本映射（可选，{@code null} 或缺省视为无显式版本）
 * @param mainClauseId     主条款主键（必须在 {@code clauseIds} 列表内）
 */
public record ClauseBinding(List<String> clauseIds, Map<String, String> clauseVersionMap, String mainClauseId) {
}
