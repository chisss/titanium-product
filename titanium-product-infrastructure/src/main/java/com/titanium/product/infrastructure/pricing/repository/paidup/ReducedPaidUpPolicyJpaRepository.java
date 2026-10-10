package com.titanium.product.infrastructure.pricing.repository.paidup;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.titanium.product.infrastructure.pricing.entity.paidup.ReducedPaidUpPolicyDO;

/** 减额缴清净费率策略 JPA 仓储（g04-05）。 */
public interface ReducedPaidUpPolicyJpaRepository extends JpaRepository<ReducedPaidUpPolicyDO, String> {

    /**
     * 按业务键（租户+产品+策略编码+策略版本）判断策略是否已存在，
     * 与唯一键 {@code uk_product_reduced_paid_up_policy_version} 对齐。
     */
    boolean existsByTenantIdAndProductIdAndPolicyCodeAndPolicyVersion(
            String tenantId, String productId, String policyCode, String policyVersion);

    /**
     * 按策略ID + 租户 + 产品查询（写侧生命周期流转的加载入口）。
     */
    Optional<ReducedPaidUpPolicyDO> findByPolicyIdAndTenantIdAndProductId(
            String policyId, String tenantId, String productId);

    @Query("""
            select p from ReducedPaidUpPolicyDO p
            where p.tenantId = :tenantId and p.productId = :productId
              and p.status = com.titanium.product.common.enums.ActuarialDefinitionStatus.PUBLISHED
              and p.effectiveFrom <= :businessTime
              and (p.effectiveTo is null or p.effectiveTo > :businessTime)
            """)
    Optional<ReducedPaidUpPolicyDO> findPublished(
            @Param("tenantId") String tenantId,
            @Param("productId") String productId,
            @Param("businessTime") LocalDateTime businessTime);
}
