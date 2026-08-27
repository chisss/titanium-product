package com.titanium.product.infrastructure.pricing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.pricing.entity.PricingPlanCommissionSchemeRefEntity;

public interface PricingPlanCommissionSchemeRefJpaRepository
        extends JpaRepository<PricingPlanCommissionSchemeRefEntity, String> {

    List<PricingPlanCommissionSchemeRefEntity> findByPlanIdOrderBySortOrderAsc(String planId);

    void deleteByPlanId(String planId);
}
