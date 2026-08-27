package com.titanium.product.infrastructure.pricing.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.titanium.product.infrastructure.pricing.entity.SurrenderValuePolicyEntity;

public interface SurrenderValuePolicyJpaRepository extends JpaRepository<SurrenderValuePolicyEntity, String> {

    @Query("""
            select p from SurrenderValuePolicyEntity p
            where p.tenantId = :tenantId and p.productId = :productId and p.policyYear = :policyYear
              and p.status = com.titanium.product.common.enums.ActuarialDefinitionStatus.PUBLISHED
              and p.effectiveFrom <= :businessTime
              and (p.effectiveTo is null or p.effectiveTo > :businessTime)
            """)
    Optional<SurrenderValuePolicyEntity> findPublished(
            @Param("tenantId") String tenantId,
            @Param("productId") String productId,
            @Param("policyYear") int policyYear,
            @Param("businessTime") LocalDateTime businessTime);
}
