package com.titanium.product.port.claim;

import java.time.LocalDateTime;

/**
 * 历史赔案条目（claim 域查询出口的防腐出参）
 * <p>
 * 🔴 本条目的**集合语义**由 claim 侧定义：返回列表即「计入 NCD 统计的赔案」——统计口径
 * （区间的开闭、拒赔是否排除等）全在 claim 侧一次定死。产品域**不得**再按 {@code claimStatus}
 * 二次过滤：两侧各过滤一次必然分叉（同一份数据在 claim 侧计入、在产品侧被剔除），
 * 反算结果的计数取证随之失真。{@code claimStatus} 仅作披露与审计，不参与判定。
 * </p>
 *
 * @param claimId     赔案ID（取证/审计回溯用）
 * @param incidentDate 出险日期
 * @param claimStatus 赔案状态码（披露用，产品域不据此过滤）
 */
public record ClaimHistoryItem(String claimId, LocalDateTime incidentDate, String claimStatus) {
}
