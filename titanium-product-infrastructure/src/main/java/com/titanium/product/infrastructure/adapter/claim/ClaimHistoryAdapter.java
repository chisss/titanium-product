package com.titanium.product.infrastructure.adapter.claim;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.titanium.claim.api.ClaimApi;
import com.titanium.claim.api.response.ClaimResponse;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.port.claim.ClaimHistoryItem;
import com.titanium.product.port.claim.ClaimHistoryPort;
import com.titanium.product.port.claim.ClaimHistoryQuery;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 历史赔案取数适配器（NCD 反算的驱动侧出口，G11/g11-03）
 *
 * <p>{@link ClaimHistoryPort} 的基础设施实现，调用理赔域 {@link ClaimApi}
 * 的 {@code GET /api/v1/claims/subject-history}（按 VIN/标的ID + 出险日期区间检索，拒赔由理赔域排除）。</p>
 *
 * <p>🔴 <b>失败语义：一律失败关闭，绝不降级为空列表</b>（g11-03 AC-03 的核心判据）。本适配器
 * <b>没有「明确不存在」这一结局</b>——理赔域在「确无赔案」时返回的是**成功 + 空列表**，
 * 那才是「无赔款」唯一合法的表达。因此其余一切形态都只能理解为「取不到」：</p>
 * <ul>
 *   <li>响应整体为 {@code null} → 取不到（防「下游 2xx 但体为空」的形态漂移）；</li>
 *   <li>信封 {@code isSuccess() == false}（HTTP 2xx 却带错误码）→ 取不到；</li>
 *   <li>{@code data == null} → 取不到（<b>空列表与 null 数据体是两回事</b>：前者是「确无赔款」，
 *       后者是「响应无效」）；</li>
 *   <li>Feign 异常（连不上 / 5xx / 超时 / 统一错误解码器包出的 {@code TitaniumRemoteException}，
 *       它 {@code extends FeignException}）→ 取不到。</li>
 * </ul>
 * <p>以上全部表达为 {@code NCD_HISTORY_UNAVAILABLE}。🔴 若把它们降级为空列表，NCD 反算会把
 * 基础设施故障当成「无赔款」并命中优待档，静默给出偏低系数⇒保费少收，且事后无从察觉
 * ——「查不到历史」与「确无赔款」是两种事实，必须可区分。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimHistoryAdapter implements ClaimHistoryPort {

    private final ClaimApi claimApi;

    @Override
    public List<ClaimHistoryItem> findIncidents(ClaimHistoryQuery query, String tenantId) {
        try {
            ApiResponse<List<ClaimResponse>> response = claimApi.getClaimsBySubjectHistory(
                    query.vin(), query.subjectId(), query.window().from(), query.window().to());
            if (response == null) {
                throw unavailable(tenantId, query, "理赔服务返回空响应");
            }
            if (!response.isSuccess()) {
                throw unavailable(tenantId, query, "响应信封为失败: code=" + response.getCode());
            }
            List<ClaimResponse> claims = response.getData();
            if (claims == null) {
                throw unavailable(tenantId, query, "响应缺少数据体（空列表与空数据体必须区分）");
            }
            return claims.stream()
                    .filter(Objects::nonNull)
                    .map(this::toItem)
                    .toList();
        } catch (FeignException e) {
            log.error("理赔域历史赔案取数失败，NCD 反算将失败关闭: tenantId={}, vin={}, subjectId={}, "
                            + "window=[{}, {}), status={}",
                    tenantId, query.vin(), query.subjectId(), query.window().from(), query.window().to(),
                    e.status(), e);
            throw unavailable(tenantId, query, "理赔服务调用失败: status=" + e.status());
        }
    }

    private ClaimHistoryItem toItem(ClaimResponse claim) {
        return new ClaimHistoryItem(claim.getClaimId(), claim.getIncidentDate(), claim.getStatus());
    }

    private PricingDomainException unavailable(String tenantId, ClaimHistoryQuery query, String reason) {
        log.error("理赔域历史赔案不可得，NCD 反算拒绝继续: tenantId={}, vin={}, subjectId={}, window=[{}, {}), reason={}",
                tenantId, query.vin(), query.subjectId(), query.window().from(), query.window().to(), reason);
        return new PricingDomainException(ProductErrorCode.NCD_HISTORY_UNAVAILABLE,
                "查不到历史赔案（与「确无赔款」是两种事实，不得按无赔款给优待）: " + reason);
    }
}
