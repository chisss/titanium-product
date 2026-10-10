package com.titanium.product.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import com.titanium.product.pricing.aggregate.paidup.ReducedPaidUpPolicy;

/**
 * 减额缴清净费率策略仓储端口（g04-05）。
 */
public interface ReducedPaidUpPolicyRepository {

    /**
     * 按业务键（租户+产品+策略编码+策略版本）判断策略是否已存在，
     * 与表唯一键 {@code uk_product_reduced_paid_up_policy_version}
     * (tenant_id, product_id, policy_code, policy_version) 对齐。
     */
    boolean existsByBusinessKey(String tenantId, String productId, String policyCode, String policyVersion);

    /**
     * 按策略ID + 租户 + 产品查询（写侧生命周期流转的加载入口）。
     */
    Optional<ReducedPaidUpPolicy> findById(String tenantId, String productId, String policyId);

    /**
     * 查询业务时点适用的已发布策略。
     */
    Optional<ReducedPaidUpPolicy> findPublished(String tenantId, String productId, LocalDateTime businessTime);

    /**
     * 保存策略（草稿创建与审批/发布/退役的生命周期流转均经此落库）。
     */
    void save(ReducedPaidUpPolicy policy);
}
