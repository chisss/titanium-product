package com.titanium.product.infrastructure.pricing.repository.premium;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.titanium.product.infrastructure.pricing.entity.premium.SurrenderValuePolicyDO;

public interface SurrenderValuePolicyJpaRepository extends JpaRepository<SurrenderValuePolicyDO, String> {

    /**
     * 按业务键（租户+产品+策略编码+策略版本+适用保单年度）判断策略是否已存在，与唯一键
     * {@code uk_product_surrender_value_version} 对齐。
     */
    boolean existsByTenantIdAndProductIdAndPolicyCodeAndPolicyVersionAndPolicyYear(
            String tenantId, String productId, String policyCode, String policyVersion, int policyYear);

    /**
     * 按策略ID + 租户 + 产品查询（写侧生命周期流转的加载入口）。
     */
    Optional<SurrenderValuePolicyDO> findByPolicyIdAndTenantIdAndProductId(
            String policyId, String tenantId, String productId);

    @Query("""
            select p from SurrenderValuePolicyDO p
            where p.tenantId = :tenantId and p.productId = :productId and p.policyYear = :policyYear
              and p.status = com.titanium.product.common.enums.ActuarialDefinitionStatus.PUBLISHED
              and p.effectiveFrom <= :businessTime
              and (p.effectiveTo is null or p.effectiveTo > :businessTime)
            """)
    Optional<SurrenderValuePolicyDO> findPublished(
            @Param("tenantId") String tenantId,
            @Param("productId") String productId,
            @Param("policyYear") int policyYear,
            @Param("businessTime") LocalDateTime businessTime);
}
