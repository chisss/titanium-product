package com.titanium.product.infrastructure.messaging.inbound;

/**
 * 赔案状态变更事件入站报文（防腐，m27-04/PKG-04）
 *
 * <p>理赔域 {@code claim-status-changed} 主题的 JSON 镜像。{@code newStatus} 是理赔域
 * {@code ClaimStatus} 枚举的序列化值（fastjson2 按枚举常量名输出，如 {@code "APPROVED"}），
 * 本域以 {@code String} 承接并原样落读模型列——<b>不引入对端枚举类型</b>。</p>
 *
 * @param claimId   赔案ID（线格式 {@code {"value":"..."}}）
 * @param tenantId  租户ID
 * @param newStatus 变更后的赔案状态（枚举常量名）
 */
public record ClaimStatusChangedMessage(
        ClaimIdRef claimId,
        String tenantId,
        String newStatus) {

    /**
     * 空安全取赔案ID字面值。
     *
     * @return 赔案ID；报文缺失该字段时为 {@code null}
     */
    public String claimIdValue() {
        return claimId == null ? null : claimId.value();
    }
}
