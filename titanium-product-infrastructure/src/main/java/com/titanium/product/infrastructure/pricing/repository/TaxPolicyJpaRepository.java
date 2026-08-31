package com.titanium.product.infrastructure.pricing.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.infrastructure.pricing.entity.TaxPolicyDO;

public interface TaxPolicyJpaRepository extends JpaRepository<TaxPolicyDO, String> {

    boolean existsByTenantIdAndProductIdAndPolicyCodeAndPolicyVersion(
            String tenantId, String productId, String policyCode, String policyVersion);

    Optional<TaxPolicyDO> findByPolicyIdAndTenantIdAndProductId(
            String policyId, String tenantId, String productId);

    List<TaxPolicyDO> findByTenantIdAndProductIdOrderByPolicyCodeAscPolicyVersionDesc(
            String tenantId, String productId);

    List<TaxPolicyDO> findByTenantIdAndProductIdAndStatusOrderByPolicyCodeAscPolicyVersionDesc(
            String tenantId, String productId, ActuarialDefinitionStatus status);

    @Query("""
            select p from TaxPolicyDO p
            where p.tenantId = :tenantId and p.productId = :productId
              and p.policyCode = :policyCode and p.policyVersion = :policyVersion
              and p.status = com.titanium.product.common.enums.ActuarialDefinitionStatus.PUBLISHED
              and p.effectiveFrom <= :businessTime
              and (p.effectiveTo is null or p.effectiveTo > :businessTime)
            """)
    Optional<TaxPolicyDO> findPublished(
            @Param("tenantId") String tenantId,
            @Param("productId") String productId,
            @Param("policyCode") String policyCode,
            @Param("policyVersion") String policyVersion,
            @Param("businessTime") LocalDateTime businessTime);
}
