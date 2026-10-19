package com.titanium.product.query.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.titanium.product.query.view.ClaimHistoryView;

/**
 * 历史赔案读模型仓储（CQRS 查询侧，m27-04/PKG-04）
 *
 * <p>派生查询逐字复刻理赔域 {@code ClaimQueryServiceImpl#getClaimsBySubjectHistory} 的过滤语义：
 * 租户 + （VIN 优先 / 标的ID 兜底）+ 出险时间半开区间 {@code [from, to)} + 排除拒赔状态。
 * 两处口径必须保持一致，否则 NCD 反算的赔案计数会与理赔域自己的检索结果漂移。</p>
 *
 * <p>🔴 <b>排除条件用 {@code StatusNot}</b>：赔案状态在投影中恒有值（created 缺省 PENDING，
 * 状态事件先到也会落状态桩），故不存在 {@code status <> ?} 漏掉 NULL 行的问题。</p>
 */
@Repository
public interface ClaimHistoryViewRepository extends JpaRepository<ClaimHistoryView, String> {

    /**
     * 按车辆识别代号检索历史赔案（NCD 主路径）。
     *
     * @param tenantId      租户ID
     * @param vin           车辆识别代号
     * @param incidentFrom  出险时间下界（含）
     * @param incidentTo    出险时间上界（不含）
     * @param excludedStatus 需排除的赔案状态（拒赔）
     * @return 命中的赔案读模型行
     */
    List<ClaimHistoryView> findByTenantIdAndVinAndIncidentDateGreaterThanEqualAndIncidentDateLessThanAndStatusNot(
            String tenantId, String vin, LocalDateTime incidentFrom, LocalDateTime incidentTo, String excludedStatus);

    /**
     * 按标的标识检索历史赔案（VIN 缺失时的兜底路径）。
     *
     * @param tenantId       租户ID
     * @param subjectId      标的标识
     * @param incidentFrom   出险时间下界（含）
     * @param incidentTo     出险时间上界（不含）
     * @param excludedStatus 需排除的赔案状态（拒赔）
     * @return 命中的赔案读模型行
     */
    List<ClaimHistoryView> findByTenantIdAndSubjectIdAndIncidentDateGreaterThanEqualAndIncidentDateLessThanAndStatusNot(
            String tenantId, String subjectId, LocalDateTime incidentFrom, LocalDateTime incidentTo,
            String excludedStatus);
}
