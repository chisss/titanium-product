package com.titanium.product.infrastructure.adapter.clause;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.titanium.clause.api.ClauseApi;
import com.titanium.clause.api.response.clause.ClauseResponse;
import com.titanium.clause.api.response.clause.CoverageResponse;
import com.titanium.common.feign.TitaniumRemoteException;
import com.titanium.metadata.enums.clause.ClauseEnum;
import com.titanium.metadata.errorcode.ClauseErrorCode;
import com.titanium.metadata.errorcode.SystemErrorCode;
import com.titanium.metadata.exception.DomainException;
import com.titanium.metadata.response.ApiResponse;
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
 * 故本域无需新增端点。</p>
 *
 * <p>m24-01a 起该契约统一返回 {@code ApiResponse} 信封：成功体经 {@code data} 取条款详情。</p>
 *
 * <p>🔴 m21-07 起「404 判据」的承载者已换人：{@code TitaniumFeignErrorDecoder} 把「非 2xx + 错误信封
 * + 非空业务码」统一包成 {@link TitaniumRemoteException}（它 {@code extends FeignException}，
 * 与 {@link FeignException.NotFound} 是<b>兄弟类</b>），故 404 不再表现为后者，须按
 * {@code ClauseErrorCode.CLAUSE_NOT_EXIST} 判码。原 {@code catch (FeignException.NotFound)}
 * 保留为兜底（下游未走统一解码器时的原生形态），但它已不是主路径——G01-R1-F2 的缺陷正是
 * 只认后者，导致「条款不存在」落到「依赖服务错误」。</p>
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
     * 取条款集合下的责任编码全集。
     * <p>
     * 逐条款调用条款域已有的 {@code GET /api/v1/clauses/{clauseId}/coverages}。与
     * {@link #checkBindable} 不同，本方法<b>没有「明确不存在」结局</b>——某条款取不到时其责任集合
     * 不可知，降级为空集会把「问不到」伪装成「该责任不属于本产品」，给用户错误的拒绝理由。
     * 故此处不设 catch，失败一律原样上抛，由编排层表达为依赖服务故障。
     * </p>
     */
    @Override
    public Set<String> listCoverageCodes(List<String> clauseIds, String tenantId) {
        if (clauseIds == null || clauseIds.isEmpty()) {
            return Set.of();
        }
        Set<String> coverageCodes = new LinkedHashSet<>();
        for (String clauseId : clauseIds) {
            ApiResponse<List<CoverageResponse>> response = clauseApi.getCoveragesByClauseId(clauseId);
            if (response == null || !response.isSuccess()) {
                String code = response != null ? response.getCode() : "null-response";
                log.error("条款责任清单查询失败, clauseId={}, tenantId={}, errorCode={}", clauseId, tenantId, code);
                throw new DomainException(SystemErrorCode.DEPENDENCY_SERVICE_ERROR,
                        "条款服务调用失败: clauseId=" + clauseId + ", errorCode=" + code);
            }
            List<CoverageResponse> coverages = response.getData();
            if (coverages == null) {
                continue;
            }
            for (CoverageResponse coverage : coverages) {
                if (coverage != null && coverage.getCoverageCode() != null) {
                    coverageCodes.add(coverage.getCoverageCode());
                }
            }
        }
        return coverageCodes;
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
            ApiResponse<ClauseResponse> response = clauseApi.getClauseById(clauseId);
            // 响应整体为 null（Provider 端不存在时抛 404，不会返回 null）：兜底防「Feign 解码异常却不抛错」
            // 的下游变更，宁可判为缺失也不放行
            if (response == null) {
                return null;
            }
            // 信封化后新增的失败形态：错误信封（非 2xx 时 Feign 已抛，此处防「2xx + 错误码」）。
            // 🔴 必须上抛而非返回 null——否则与「条款不存在」混为一谈，正是本类三结局分离要禁止的。
            if (!response.isSuccess()) {
                log.error("条款详情查询失败, clauseId={}, tenantId={}, error={}",
                        clauseId, tenantId, response.getMessage());
                throw new DomainException(SystemErrorCode.DEPENDENCY_SERVICE_ERROR,
                        "条款服务调用失败: clauseId=" + clauseId + ", errorCode=" + response.getCode());
            }
            return response.getData();
        } catch (TitaniumRemoteException e) {
            // 🔴 m21-07 起 common 侧统一下发 TitaniumFeignErrorDecoder：下游「非 2xx + ApiResponse 错误体
            //    + 非空业务码」一律被包成 TitaniumRemoteException。它 extends FeignException（与
            //    FeignException.NotFound 是**兄弟类**而非其子类），故下面那个 catch (FeignException.NotFound)
            //    **接不到它**——条款域的 404 走的正是这条路（ClauseExceptionHandler 按 ErrorCategoryHttpStatus
            //    把 CLAUSE_NOT_EXIST 映射成 404 + 错误信封）。
            //    不在此判码，CLAUSE_NOT_EXIST 会落到 catch (FeignException) 上抛，被外层表达成
            //    「依赖服务错误」，把「条款不存在」误报成基础设施故障——两者错误码不同，排查方向完全不同。
            if (ClauseErrorCode.CLAUSE_NOT_EXIST.getCode().equals(e.getRemoteCode())) {
                log.warn("条款不存在，产品条款绑定校验将拒绝该引用: clauseId={}, tenantId={}", clauseId, tenantId);
                return null;
            }
            log.error("条款服务调用失败，条款可绑定性核查无法完成: clauseId={}, tenantId={}, status={}, remoteCode={}",
                    clauseId, tenantId, e.status(), e.getRemoteCode(), e);
            throw e;
        } catch (FeignException.NotFound e) {
            // 兜底：下游未走 TitaniumFeignErrorDecoder（如 404 体不是 ApiResponse 信封）时的原生形态
            log.warn("条款不存在，产品条款绑定校验将拒绝该引用: clauseId={}, tenantId={}", clauseId, tenantId);
            return null;
        } catch (FeignException e) {
            log.error("条款服务调用失败，条款可绑定性核查无法完成: clauseId={}, tenantId={}, status={}",
                    clauseId, tenantId, e.status(), e);
            throw e;
        }
    }
}
