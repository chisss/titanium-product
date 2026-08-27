package com.titanium.product.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.titanium.product.aggregate.PricingPlanDefinition;
import com.titanium.product.common.enums.PricingPlanStatus;

/**
 * Product 定价方案仓储端口。
 */
public interface PricingPlanRepository {

    boolean existsByBusinessKey(String tenantId, String productId, String planVersion);

    Optional<PricingPlanDefinition> findById(String tenantId, String productId, String planId);

    Optional<PricingPlanDefinition> findByVersion(String tenantId, String productId, String planVersion);

    List<PricingPlanDefinition> findAll(String tenantId, String productId, PricingPlanStatus status);

    Optional<PricingPlanDefinition> findEffective(
            String tenantId, String productId, String currency, LocalDateTime businessTime);

    boolean existsPublishedOverlap(
            String tenantId,
            String productId,
            String excludedPlanId,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo);

    void save(PricingPlanDefinition pricingPlan);
}
