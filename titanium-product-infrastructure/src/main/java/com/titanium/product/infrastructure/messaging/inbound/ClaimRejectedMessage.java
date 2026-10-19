package com.titanium.product.infrastructure.messaging.inbound;

/**
 * 赔案拒赔事件入站报文（防腐，m27-04/PKG-04）
 *
 * <p>理赔域 {@code claim-rejected} 主题的 JSON 镜像。🔴 <b>拒赔是 NCD 反算唯一需要主动识别的状态</b>：
 * 理赔域自己的历史赔案检索就把 {@code REJECTED} 排除在外（拒赔不计入赔款次数），本投影必须复刻该口径。
 * 理赔域的拒赔路径<b>只发本事件、不发 {@code claim-status-changed}</b>，故本主题不可省略。</p>
 *
 * @param claimId  赔案ID（线格式 {@code {"value":"..."}}）
 * @param tenantId 租户ID
 */
public record ClaimRejectedMessage(
        ClaimIdRef claimId,
        String tenantId) {

    /**
     * 空安全取赔案ID字面值。
     *
     * @return 赔案ID；报文缺失该字段时为 {@code null}
     */
    public String claimIdValue() {
        return claimId == null ? null : claimId.value();
    }
}
