package com.titanium.product.port.clause;

import java.util.List;
import java.util.Set;

/**
 * 条款可绑定性核查端口
 *
 * <p>产品绑定条款时，{@code ProductClauseRel.clauseId} 指向的是条款域聚合根的主键，而产品域
 * <b>无从自行判定该值的真伪，也无从知道它当前是否在有效期内</b>——本域只有「产品-条款关联」
 * 读模型，不含条款主数据。写入侧因此在此向条款域求证，一次往返回答两件事：该条款
 * <b>是否存在</b>、<b>状态是否允许绑定</b>。</p>
 *
 * <p>🔴 <b>为什么必须有这道校验</b>：条款绑定的既有校验只到「{@code clauseIds} 非空 +
 * 主条款在列表内」，<b>任何字符串都能落库</b>。R7-01 的故障正是由此而来——条款
 * <b>编码</b>被当成条款<b>主键</b>写进绑定关系，读侧拿它查条款详情必然 404，
 * 产品详情页随之整页报「资源不存在」，出单链路的条款/责任快照也一并取空。</p>
 *
 * <p>🔴 <b>形态校验（如「必须是 UUID」）堵不住这个洞，故不采用</b>：条款主键既有 UUID 形态
 * （{@code 7d7c8bfe-c10f-4096-8424-9b9a6e38f982}）也有雪花数字 ID 形态
 * （{@code 221612854808150016}），按 UUID 正则判会把全部雪花 ID 的合法条款挡在门外；
 * 且「形态合法但不存在」的主键同样有害。唯一可靠的判据是<b>向条款域实地求证</b>。</p>
 *
 * <p>🔴 <b>绑定要求条款处于 ACTIVE</b>（用户 2026-09-22 裁定；与创建产品时
 * {@code validateTemplateActive} 要求模板 ACTIVE 对称）。判据不是「形态」而是「状态」：
 * 未生效的条款其版本尚未定稿，可被继续修订，而 {@code ProductClauseRel.clauseVersion} 的设计
 * 意图正是「绑定具体版本、隔离后续修订」——绑定一条仍可被改写的条款，等于绑了一个会变的版本。
 * DRAFT / PENDING_APPROVAL / INACTIVE / EXPIRED / ARCHIVED 一律拒绝。</p>
 *
 * <p>六边形定位：接口是内核边界概念，与 {@code aggregate} 平级置于 {@code domain/port}，
 * 并按对端域分包（项目规约 §3.4.6 / §3.4.13）；实现 {@code ClauseExistenceAdapter} 置于
 * infrastructure 层。领域服务不得依赖本端口（ArchUnit {@code domainServiceShouldNotDependOnPort}），
 * 故校验的<b>编排</b>落在 application 层。</p>
 */
public interface ClauseExistencePort {

    /**
     * 核查给定的条款引用是否可作为产品绑定目标。
     *
     * @param clauseIds 待核查的条款引用（条款主键），可为 {@code null} 或空
     * @param tenantId  租户ID
     * @return 核查结论；两种拒绝情形<b>分别装在不同的清单里</b>，全部合规时返回
     *         {@link ClauseBindingCheck#clean()}
     * @throws RuntimeException 条款服务不可用时原样抛出（<b>不得</b>把基础设施故障表达成「条款不存在」）
     */
    ClauseBindingCheck checkBindable(List<String> clauseIds, String tenantId);

    /**
     * 取给定条款集合下**全部保险责任**的编码全集。
     *
     * <p>用于「责任级保额档次」配置的归属校验：一条档次的 {@code coverageCode} 必须落在该产品
     * 已绑定条款的责任集合内，否则这条配置永远不会被投保侧命中——产品侧配了，投保侧却匹配不到，
     * 属于典型的「配了没用」。产品域只有「产品-条款关联」，不含条款主数据，故须向条款域取责任清单。</p>
     *
     * <p>🔴 <b>复用条款域已有端点</b>（{@code GET /api/v1/clauses/{clauseId}/coverages}），
     * <b>不新增跨域契约</b>——该端点本就为上游取责任明细而存在（policy 出单装配责任快照即用它）。</p>
     *
     * <p>🔴 <b>与 {@link #checkBindable} 的失败语义刻意不同</b>：本方法<b>没有「明确不存在」这一结局</b>。
     * 某条款取不到时，其责任集合是<b>不可知</b>的，若当成空集处理，就会把「问不到」静默降级成
     * 「该责任不属于本产品」，进而给用户一个错误的拒绝理由（正是 {@code checkBindable} 三结局分离
     * 要禁止的那类混淆）。故本方法<b>一律原样上抛</b>，由编排层表达为依赖服务故障。</p>
     *
     * @param clauseIds 条款引用（条款主键），可为 {@code null} 或空
     * @param tenantId  租户ID
     * @return 责任编码全集（去重）；条款集合为空时返回空集
     * @throws RuntimeException 条款服务不可用、或某条款取不到时原样抛出（<b>不降级为空集</b>）
     */
    Set<String> listCoverageCodes(List<String> clauseIds, String tenantId);
}
