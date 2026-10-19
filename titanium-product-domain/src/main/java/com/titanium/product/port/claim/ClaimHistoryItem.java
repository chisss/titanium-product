package com.titanium.product.port.claim;

import java.time.LocalDateTime;

/**
 * 历史赔案条目（收口理赔域赔案检索口径的出参）
 * <p>
 * 🔴 本条目的**集合语义** = 理赔域「按车辆/标的检索历史赔案」口径的**逐字复刻**
 * （租户 + VIN 优先/标的ID 兜底 + 出险区间半开 + 排除拒赔）。过滤在实现侧查询时一次定死
 * （m27-04/PKG-04 起为查本地投影 {@code t_product_claim_history_view}）。产品域**不得**再按
 * {@code claimStatus} 二次过滤：两侧各过滤一次必然分叉（同一份数据在一侧计入、另一侧被剔除），
 * 反算结果的计数取证随之失真。{@code claimStatus} 仅作披露与审计，不参与判定。
 * </p>
 *
 * @param claimId     赔案ID（取证/审计回溯用）
 * @param incidentDate 出险日期
 * @param claimStatus 赔案状态码（披露用，产品域不据此过滤）
 */
public record ClaimHistoryItem(String claimId, LocalDateTime incidentDate, String claimStatus) {
}
