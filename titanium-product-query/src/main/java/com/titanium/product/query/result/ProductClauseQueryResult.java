package com.titanium.product.query.result;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/**
 * 产品条款关联查询结果（CQRS 读侧 DTO）
 * <p>
 * 表达产品绑定的单条条款关联，作为查询返回的稳定契约，与写侧领域实体
 * {@code ProductClauseRel} 解耦，不泄漏持久化细节。
 * </p>
 */
@Getter
@Setter
public class ProductClauseQueryResult {

    /** 关联的条款ID（指向条款域聚合根） */
    private String clauseId;

    /** 条款版本（绑定具体版本，避免条款修订影响产品） */
    private String clauseVersion;

    /** 是否为主条款（一个产品仅一个主条款，其余为附加条款） */
    private Boolean mainClause;

    /** 绑定时间 */
    private LocalDateTime bindTime;
}
