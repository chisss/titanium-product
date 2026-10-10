package com.titanium.product.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import com.titanium.product.pricing.aggregate.ncd.NcdRuleSet;

/**
 * NCD（无赔款优待）规则集仓储端口（G11/g11-03）。
 * <p>
 * 形态与 {@link PolicyLoanPolicyRepository} 同构：同一租户 + 产品在任一时点**至多一个**已发布规则集。
 * 若未来业务要求按险别/车型族并存多套规则集，须在此增补区分键（并同步表唯一键与取数判据），
 * 不得让 {@link #findPublished} 在多行命中时静默取其一。
 * </p>
 */
public interface NcdRuleSetRepository {

    /**
     * 按业务键（租户+产品+规则集编码+规则集版本）判断规则集是否已存在，
     * 与表唯一键 {@code uk_product_ncd_rule_set_version(tenant_id, product_id, rule_set_code, rule_set_version)} 对齐。
     */
    boolean existsByBusinessKey(String tenantId, String productId, String ruleSetCode, String ruleSetVersion);

    /**
     * 按规则集ID + 租户 + 产品查询（写侧生命周期流转的加载入口）。
     */
    Optional<NcdRuleSet> findById(String tenantId, String productId, String ruleSetId);

    /**
     * 查询业务时点适用的已发布规则集（含其全部档位）。
     * <p>
     * 返回空表示「该产品未配置 NCD 规则」——调用方须以
     * {@code NCD_RULE_NOT_CONFIGURED} 显式拒绝，**不得**按无赔款优待不适用而以系数 1.0 兜底。
     * </p>
     */
    Optional<NcdRuleSet> findPublished(String tenantId, String productId, LocalDateTime businessTime);

    /**
     * 保存规则集（草稿创建与审批/发布/退役的生命周期流转均经此落库；档位随规则集整体替换）。
     */
    void save(NcdRuleSet ruleSet);
}
