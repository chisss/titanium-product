package com.titanium.product.infrastructure.adapter.clause;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.titanium.clause.api.ClauseApi;
import com.titanium.clause.api.response.clause.ClauseResponse;
import com.titanium.metadata.enums.clause.ClauseEnum;
import com.titanium.product.port.clause.ClauseBindingCheck;
import com.titanium.product.port.clause.ClauseExistencePort;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 条款可绑定性核查适配器
 *
 * <p>{@link ClauseExistencePort} 的基础设施实现，调用条款域 {@link ClauseApi}（Feign）。
 * 复用条款域<b>已有</b>的 {@code GET /api/v1/clauses/{clauseId}}——条款不存在时该端点返回
 * 404（{@code ClauseErrorCode.CLAUSE_NOT_EXIST}，经条款域 {@code ClauseExceptionHandler} 映射），
 * 这正是「确不存在」唯一可靠的判据；条款存在时响应体自带 {@code status}，状态判据<b>零额外往返</b>，
 * 故条款域本次<b>零改动</b>。</p>
 *
 * <p>🔴 <b>三种结局必须分开处理</b>（R7-01 的教训——把失败说成「资源不存在」会让排查方向被带偏）：</p>
 * <ul>
 *   <li><b>存在且状态允许</b>：放行；</li>
 *   <li><b>明确不存在</b>：条款域回了 404 —— 记入 {@code missingClauseIds}，交编排层拒绝；</li>
 *   <li><b>存在但未生效</b>：状态非 {@code ACTIVE} —— 记入 {@code nonActiveClauseIds}，
 *       <b>与上者分开</b>，否则运营会照「条款不存在」的提示去新建同编码条款，撞唯一约束；</li>
 *   <li><b>问不到</b>（连不上 / 5xx / 超时）：<b>原样上抛</b>，绝不可记成「不存在」。否则用户会看到
 *       「条款不存在: &lt;一个完全合法的ID&gt;」，把基础设施故障伪装成输入错误。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClauseExistenceAdapter implements ClauseExistencePort {

    private final ClauseApi clauseApi;

    @Override
    public ClauseBindingCheck checkBindable(List<String> clauseIds, String tenantId) {
        if (clauseIds == null || clauseIds.isEmpty()) {
            return ClauseBindingCheck.clean();
        }
        List<String> missing = new ArrayList<>();
        List<String> nonActive = new ArrayList<>();
        for (String clauseId : clauseIds) {
            ClauseResponse clause = fetch(clauseId, tenantId);
            if (clause == null) {
                missing.add(clauseId);
            } else if (!ClauseEnum.ClauseStatus.ACTIVE.equals(clause.getStatus())) {
                log.warn("条款状态不允许绑定，产品条款绑定校验将拒绝该引用: clauseId={}, status={}, tenantId={}",
                        clauseId, clause.getStatus(), tenantId);
                nonActive.add(clauseId);
            }
        }
        return new ClauseBindingCheck(missing, nonActive);
    }

    /**
     * 取条款详情。
     *
     * @param clauseId 条款主键
     * @param tenantId 租户ID
     * @return 条款详情；条款域明确回答「不存在」时返回 {@code null}
     */
    private ClauseResponse fetch(String clauseId, String tenantId) {
        try {
            // Provider 端不存在时抛 ClauseNotFoundException（404），不会返回 null；
            // 此处的 null 兜底是防「Feign 解码异常却未抛错」的下游变更，宁可判为缺失也不放行
            return clauseApi.getClauseById(clauseId);
        } catch (FeignException.NotFound e) {
            log.warn("条款不存在，产品条款绑定校验将拒绝该引用: clauseId={}, tenantId={}", clauseId, tenantId);
            return null;
        } catch (FeignException e) {
            log.error("条款服务调用失败，条款可绑定性核查无法完成: clauseId={}, tenantId={}, status={}",
                    clauseId, tenantId, e.status(), e);
            throw e;
        }
    }
}
