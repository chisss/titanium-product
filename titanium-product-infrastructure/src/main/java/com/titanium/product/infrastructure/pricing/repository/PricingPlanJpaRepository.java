package com.titanium.product.infrastructure.pricing.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.titanium.product.common.enums.PricingPlanStatus;
import com.titanium.product.infrastructure.pricing.entity.PricingPlanDO;

/**
 * 定价方案 JPA 仓储。
 */
public interface PricingPlanJpaRepository extends JpaRepository<PricingPlanDO, String> {

    boolean existsByTenantIdAndProductIdAndPlanVersion(
            String tenantId, String productId, String planVersion);

    Optional<PricingPlanDO> findByPlanIdAndTenantIdAndProductId(
            String planId, String tenantId, String productId);

    Optional<PricingPlanDO> findByTenantIdAndProductIdAndPlanVersion(
            String tenantId, String productId, String planVersion);

    List<PricingPlanDO> findByTenantIdAndProductIdOrderByCreateTimeDesc(
            String tenantId, String productId);

    List<PricingPlanDO> findByTenantIdAndProductIdAndStatusOrderByCreateTimeDesc(
            String tenantId, String productId, PricingPlanStatus status);

    @Query("""
            select plan from PricingPlanDO plan
             where plan.tenantId = :tenantId
               and plan.productId = :productId
               and plan.currency = :currency
               and plan.status = com.titanium.product.common.enums.PricingPlanStatus.PUBLISHED
               and plan.effectiveFrom <= :businessTime
               and (plan.effectiveTo is null or :businessTime < plan.effectiveTo)
            """)
    List<PricingPlanDO> findEffectivePlans(
            @Param("tenantId") String tenantId,
            @Param("productId") String productId,
            @Param("currency") String currency,
            @Param("businessTime") LocalDateTime businessTime);

    @Query("""
            select count(plan) from PricingPlanDO plan
             where plan.tenantId = :tenantId
               and plan.productId = :productId
               and plan.planId <> :excludedPlanId
               and plan.currency = :currency
               and plan.status = com.titanium.product.common.enums.PricingPlanStatus.PUBLISHED
               and (:effectiveTo is null or plan.effectiveFrom < :effectiveTo)
               and (plan.effectiveTo is null or plan.effectiveTo > :effectiveFrom)
            """)
    long countPublishedOverlaps(
            @Param("tenantId") String tenantId,
            @Param("productId") String productId,
            @Param("excludedPlanId") String excludedPlanId,
            @Param("currency") String currency,
            @Param("effectiveFrom") LocalDateTime effectiveFrom,
            @Param("effectiveTo") LocalDateTime effectiveTo);
}
