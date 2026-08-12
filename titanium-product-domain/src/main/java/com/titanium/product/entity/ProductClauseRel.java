package com.titanium.product.entity;

import java.time.LocalDateTime;

/**
 * 产品-条款关联实体 表示产品和条款之间的关联关系，包含条款ID、版本、是否主条款等信息
 * <p>
 * 采用 record 定义（聚合内不可变值对象，符合 §3.4.1）：完整 5 字段的规范构造器供事件溯源/
 * Jackson 反序列化重建（事件流回放读取全部字段），业务侧用下方 3 参便捷构造器由条款ID+版本派生
 * {@code relId}、绑定时刻取 {@code now()}。此前为普通 class 且仅有派生构造器，导致读侧投影回放
 * {@code ProductCreatedEvent} 时 Jackson 无可用 Creator 而反序列化失败，读模型 t_product_view 空。
 * </p>
 *
 * @param relId 相对标识（聚合内唯一，如 条款ID+版本）
 * @param clauseId 关联的条款ID（指向条款域聚合根）
 * @param clauseVersion 条款版本（绑定具体版本的条款，避免条款修订影响产品）
 * @param isMainClause 是否为主条款（一个产品仅一个主条款，其余为附加条款）
 * @param bindTime 绑定时间
 */
public record ProductClauseRel(String relId, String clauseId, String clauseVersion,
        Boolean isMainClause, LocalDateTime bindTime) {

    /**
     * 业务便捷构造器：由条款ID与版本派生相对标识，绑定时间取当前时刻
     *
     * @param clauseId 条款ID
     * @param clauseVersion 条款版本
     * @param isMainClause 是否为主条款
     */
    public ProductClauseRel(String clauseId, String clauseVersion, boolean isMainClause) {
        this(clauseId + "_" + clauseVersion, clauseId, clauseVersion, isMainClause, LocalDateTime.now());
    }
}
