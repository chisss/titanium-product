package com.titanium.product.infrastructure.pricing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.infrastructure.pricing.entity.PremiumCalculationEntity;

/**
 * Product 确认计算 JPA 仓储。
 */
public interface PremiumCalculationJpaRepository extends JpaRepository<PremiumCalculationEntity, String> {

    Optional<PremiumCalculationEntity> findByCalculationIdAndTenantId(
            String calculationId, String tenantId);

    Optional<PremiumCalculationEntity> findByTenantIdAndCalculationRequestIdAndPurpose(
            String tenantId, String calculationRequestId, PricingCalculationPurpose purpose);
}
