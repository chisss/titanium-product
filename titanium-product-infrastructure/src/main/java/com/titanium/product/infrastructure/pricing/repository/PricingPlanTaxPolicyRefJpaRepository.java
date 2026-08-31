package com.titanium.product.infrastructure.pricing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.pricing.entity.PricingPlanTaxPolicyRefDO;

public interface PricingPlanTaxPolicyRefJpaRepository
        extends JpaRepository<PricingPlanTaxPolicyRefDO, String> {

    List<PricingPlanTaxPolicyRefDO> findByPlanIdOrderBySortOrderAsc(String planId);

    void deleteByPlanId(String planId);
}
