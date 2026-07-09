package com.titanium.product.entity;

import java.time.LocalDateTime;

import lombok.Getter;

/**
 * 产品-条款关联实体 表示产品和条款之间的关联关系，包含条款ID、版本、是否主条款等信息
 */
@Getter
public class ProductClauseRel {
    // 相对标识（聚合内唯一，如 条款ID+版本）
    private final String        relId;
    // 关联的条款ID（指向条款域聚合根）
    private final String        clauseId;
    // 条款版本（绑定具体版本的条款，避免条款修订影响产品）
    private final String        clauseVersion;
    // 是否为主条款（一个产品仅一个主条款，其余为附加条款）
    private final Boolean       isMainClause;
    // 绑定时间
    private final LocalDateTime bindTime;

    /**
     * 构造函数
     *
     * @param clauseId 条款ID
     * @param clauseVersion 条款版本
     * @param isMainClause 是否为主条款
     */
    public ProductClauseRel(String clauseId, String clauseVersion, boolean isMainClause) {
        this.relId = clauseId + "_" + clauseVersion;
        this.clauseId = clauseId;
        this.clauseVersion = clauseVersion;
        this.isMainClause = isMainClause;
        this.bindTime = LocalDateTime.now();
    }
}
