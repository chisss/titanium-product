package com.titanium.product.api.response.clause;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 产品条款关联对外响应（Feign 契约）
 * <p>
 * 供 policy 域出单时取「本产品绑定了哪些条款、各是什么版本」，据此向 clause 域取条款与责任，
 * 装配保单的条款快照（L2.5）与责任快照（L4）。
 * </p>
 * <p>
 * 条款<b>版本</b>是本响应的关键字段：产品绑定的是具体版本的条款，条款域后续修订产生新版本
 * 不影响已按旧版本承保的存量保单。
 * </p>
 */
@Data
public class ProductClauseResponse {

    /** 关联的条款ID（指向 clause 域聚合根） */
    private String        clauseId;

    /** 条款版本（绑定具体版本，避免条款修订影响已出保单） */
    private String        clauseVersion;

    /** 是否为主条款（一个产品仅一个主条款，其余为附加条款） */
    private Boolean       mainClause;

    /** 绑定时间 */
    private LocalDateTime bindTime;
}
