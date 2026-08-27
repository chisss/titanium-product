package com.titanium.product.infrastructure.pricing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.pricing.entity.PricingPlanTaxPolicyRefEntity;

public interface PricingPlanTaxPolicyRefJpaRepository
        extends JpaRepository<PricingPlanTaxPolicyRefEntity, String> {

    List<PricingPlanTaxPolicyRefEntity> findByPlanIdOrderBySortOrderAsc(String planId);

    void deleteByPlanId(String planId);
}
