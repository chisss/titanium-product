package com.titanium.product.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import com.titanium.product.pricing.aggregate.surrender.SurrenderValuePolicy;

/**
 * 退保价值策略仓储端口。
 */
public interface SurrenderValuePolicyRepository {

    /**
     * 按业务键（租户+产品+策略编码+策略版本+适用保单年度）判断策略是否已存在。
     * <p>
     * 🔴 须含 {@code policyYear}：与表唯一键
     * {@code uk_product_surrender_value_version(tenant_id, product_id, policy_code, policy_version, policy_year)}
     * 对齐，否则同一编码版本下的不同保单年度策略会被误判为重复。
     * </p>
     */
    boolean existsByBusinessKey(
            String tenantId, String productId, String policyCode, String policyVersion, int policyYear);

    /**
     * 按策略ID + 租户 + 产品查询（写侧生命周期流转的加载入口）。
     */
    Optional<SurrenderValuePolicy> findById(String tenantId, String productId, String policyId);

    /**
     * 查询业务时点适用的已发布策略。
     */
    Optional<SurrenderValuePolicy> findPublished(
            String tenantId, String productId, int policyYear, LocalDateTime businessTime);

    /**
     * 保存策略（草稿创建与审批/发布/退役的生命周期流转均经此落库）。
     */
    void save(SurrenderValuePolicy policy);
}
