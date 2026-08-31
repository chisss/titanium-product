package com.titanium.product.infrastructure.pricing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.pricing.entity.PricingPlanDynamicFactorRefDO;

public interface PricingPlanDynamicFactorRefJpaRepository
        extends JpaRepository<PricingPlanDynamicFactorRefDO, String> {

    List<PricingPlanDynamicFactorRefDO> findByPlanIdOrderBySortOrderAsc(String planId);

    void deleteByPlanId(String planId);
}
