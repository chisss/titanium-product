package com.titanium.product.infrastructure.pricing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.pricing.entity.PricingPlanDynamicFactorRefEntity;

public interface PricingPlanDynamicFactorRefJpaRepository
        extends JpaRepository<PricingPlanDynamicFactorRefEntity, String> {

    List<PricingPlanDynamicFactorRefEntity> findByPlanIdOrderBySortOrderAsc(String planId);

    void deleteByPlanId(String planId);
}
