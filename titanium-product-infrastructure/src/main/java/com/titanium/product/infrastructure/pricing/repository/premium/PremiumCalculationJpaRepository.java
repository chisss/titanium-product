package com.titanium.product.infrastructure.pricing.repository.premium;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.infrastructure.pricing.entity.premium.PremiumCalculationDO;

/**
 * Product 确认计算 JPA 仓储。
 */
public interface PremiumCalculationJpaRepository extends JpaRepository<PremiumCalculationDO, String> {

    Optional<PremiumCalculationDO> findByCalculationIdAndTenantId(
            String calculationId, String tenantId);

    Optional<PremiumCalculationDO> findByTenantIdAndCalculationRequestIdAndPurpose(
            String tenantId, String calculationRequestId, PricingCalculationPurpose purpose);
}
