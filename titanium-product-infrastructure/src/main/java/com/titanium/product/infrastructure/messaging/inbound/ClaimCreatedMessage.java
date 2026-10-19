package com.titanium.product.infrastructure.messaging.inbound;

import java.time.LocalDateTime;

/**
 * 赔案创建事件入站报文（防腐，m27-04/PKG-04）
 *
 * <p>理赔域 {@code claim-created} 主题的 JSON 镜像，仅承接 NCD 反算所需的四个要素
 * （VIN / 标的ID / 出险时间 / 租户）。未声明的对端字段由 fastjson2 忽略——本域不关心赔案金额、
 * 单证等其余内容，多承接一个字段就多一份随对端演进漂移的耦合面。</p>
 *
 * @param claimId      赔案ID（线格式 {@code {"value":"..."}}）
 * @param tenantId     租户ID
 * @param subjectId    标的标识（可能为空：历史事件）
 * @param vin          车辆识别代号（可能为空：历史事件）
 * @param incidentDate 出险时间（可能为空：历史事件）
 */
public record ClaimCreatedMessage(
        ClaimIdRef claimId,
        String tenantId,
        String subjectId,
        String vin,
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
