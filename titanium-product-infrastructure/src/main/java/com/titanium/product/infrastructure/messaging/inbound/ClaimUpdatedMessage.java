package com.titanium.product.infrastructure.messaging.inbound;

import java.time.LocalDateTime;

/**
 * 赔案更新事件入站报文（防腐，m27-04/PKG-04）
 *
 * <p>理赔域 {@code claim-updated} 主题的 JSON 镜像。该事件当前只携带可更正的出险信息，
 * 本域仅承接 {@code incidentDate}——它是 NCD 统计区间的过滤列，漏接会让区间统计口径与理赔域漂移。</p>
 *
 * @param claimId      赔案ID（线格式 {@code {"value":"..."}}）
 * @param tenantId     租户ID
 * @param incidentDate 更正后的出险时间
 */
public record ClaimUpdatedMessage(
        ClaimIdRef claimId,
        String tenantId,
        LocalDateTime incidentDate) {

    /**
     * 空安全取赔案ID字面值。
     *
     * @return 赔案ID；报文缺失该字段时为 {@code null}
     */
    public String claimIdValue() {
        return claimId == null ? null : claimId.value();
    }
}
