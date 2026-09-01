package com.titanium.product.repository;

import java.util.Optional;

import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.pricing.aggregate.PremiumCalculation;

/**
 * Product 确认计算仓储端口。
 */
public interface PremiumCalculationRepository {

    Optional<PremiumCalculation> findById(String tenantId, String calculationId);

    Optional<PremiumCalculation> findByIdempotencyKey(
            String tenantId, String calculationRequestId, PricingCalculationPurpose purpose);

    void save(PremiumCalculation calculation);
}
