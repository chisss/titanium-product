package com.titanium.product.infrastructure.adapter.claim;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.port.claim.ClaimHistoryItem;
import com.titanium.product.port.claim.ClaimHistoryPort;
import com.titanium.product.port.claim.ClaimHistoryQuery;
import com.titanium.product.query.repository.ClaimHistoryViewRepository;
import com.titanium.product.query.view.ClaimHistoryView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 历史赔案取数适配器（NCD 反算的驱动侧出口，G11/g11-03；m27-04/PKG-04 改本地投影）
 *
 * <p>{@link ClaimHistoryPort} 的基础设施实现。🔴 <b>不再调用理赔域服务</b>：赔案事实由
 * {@code ClaimHistoryEventListener} 消费理赔域事件投影到本域读模型
 * {@code t_product_claim_history_view}，本适配器只查本地表。原实现直接 Feign 调理赔域
 * {@code ClaimApi#getClaimsBySubjectHistory}，是服务依赖图 {@code product → claim} 反馈边的唯一来源
 * （与 {@code policy → billing → product} 共同构成环）；改为本地投影后该反馈边消失，见 ADR-0002。</p>
 *
 * <p>查询语义与理赔域自身的「按车辆/标的检索历史赔案」<b>逐字对齐</b>：租户 + VIN（优先）或标的ID +
 * 出险时间半开区间 {@code [from, to)} + 排除拒赔（{@code REJECTED}）。两处口径若漂移，同一标的下
 * NCD 计数会与理赔域检索结果不一致。</p>
 *
 * <h3>🔴 失败语义：取不到一律失败关闭，绝不降级为空列表</h3>
 * <p>本适配器<b>没有「明确不存在」这一结局</b>——本地读模型返回空列表才是「确无赔款」唯一合法的
 * 表达。因此其余形态都只能理解为「取不到」，并抛 {@code NCD_HISTORY_UNAVAILABLE}：</p>
 * <ul>
 *   <li>读模型查询失败（数据库不可用、SQL 错误等 {@link DataAccessException}）→ 取不到；</li>
 *   <li>VIN 与标的ID皆空 → 调用方错误（{@code NCD_SUBJECT_IDENTIFIER_REQUIRED}），
 *       <b>不得</b>当成「查无赔案」（空参数会退化成 IS NULL 查询）。</li>
 * </ul>
 * <p>若把基础设施故障降级为空列表，NCD 反算会把「查不到」当成「无赔款」命中优待档，静默给出
 * 偏低系数 ⇒ 保费少收，且事后无从察觉。</p>
 *
 * <h3>⚠️ 投影延迟与历史回填（语义变化，登记在案）</h3>
 * <ul>
 *   <li><b>最终一致窗口</b>：赔案创建/状态变更经 Kafka 投影到本域，存在秒级延迟。刚发生的赔案在
 *       窗口内可能不被计入 —— 方向是「少数一次赔款」⇒ NCD 优待偏高。窗口远小于 NCD 的
 *       {@code lookbackMonths} 量级，且投影失败会在重试后自动补齐。</li>
 *   <li><b>保留期外的历史</b>：消费组首次启动从最早位点消费，但 Kafka 保留期（默认 7 天）之外的
 *       历史赔案不会出现在投影中。NCD 回看窗口通常 ≥12 个月，故 R-3 启用前<b>必须完成一次历史回填</b>
 *       （从理赔域读模型导出一批赔案写入本表，属数据作业，不在本适配器的运行路径上）。</li>
 *   <li><b>拒赔后结案不可见</b>：理赔域 {@code claim-closed} 主题无发布点且事件缺 tenantId，本投影
 *       无法观察 REJECTED→CLOSED，已拒赔案件永久保持 REJECTED（与 NCD「拒赔不计入」业务规则一致；
 *       与理赔域自身检索在「拒赔后结案」这一路径上存在口径差异，见 ADR-0002）。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimHistoryAdapter implements ClaimHistoryPort {

    /** 拒赔状态线格式（理赔域 {@code ClaimStatus.REJECTED} 按枚举名序列化） */
    private static final String STATUS_REJECTED = "REJECTED";

    private final ClaimHistoryViewRepository claimHistoryViewRepository;

    @Override
    public List<ClaimHistoryItem> findIncidents(ClaimHistoryQuery query, String tenantId) {
        boolean hasVin = query.vin() != null && !query.vin().isBlank();
        if (!hasVin && (query.subjectId() == null || query.subjectId().isBlank())) {
            // 空标识不得退化成「查无赔案」：派生查询会把 null 参数生成为 IS NULL，命中状态桩行
            throw new PricingDomainException(ProductErrorCode.NCD_SUBJECT_IDENTIFIER_REQUIRED,
                    "按车辆/标的检索历史赔案时 VIN 与标的ID至少须提供一个");
        }
        try {
            List<ClaimHistoryView> views = hasVin
                    ? claimHistoryViewRepository
                            .findByTenantIdAndVinAndIncidentDateGreaterThanEqualAndIncidentDateLessThanAndStatusNot(
                                    tenantId, query.vin(), query.window().from(), query.window().to(),
                                    STATUS_REJECTED)
                    : claimHistoryViewRepository
                            .findByTenantIdAndSubjectIdAndIncidentDateGreaterThanEqualAndIncidentDateLessThanAndStatusNot(
                                    tenantId, query.subjectId(), query.window().from(), query.window().to(),
                                    STATUS_REJECTED);
            return views.stream()
                    .map(view -> new ClaimHistoryItem(view.getClaimId(), view.getIncidentDate(), view.getStatus()))
                    .toList();
        } catch (DataAccessException e) {
            log.error("本地赔案读模型查询失败，NCD 反算将失败关闭: tenantId={}, vin={}, subjectId={}, window=[{}, {})",
                    tenantId, query.vin(), query.subjectId(), query.window().from(), query.window().to(), e);
            throw new PricingDomainException(ProductErrorCode.NCD_HISTORY_UNAVAILABLE,
                    "查不到历史赔案（与「确无赔款」是两种事实，不得按无赔款给优待）: 本地赔案读模型查询失败");
        }
    }
}
