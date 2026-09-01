package com.titanium.product.infrastructure.pricing.repository.pricing;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.pricing.entity.pricing.PricingPlanCommissionSchemeRefDO;

public interface PricingPlanCommissionSchemeRefJpaRepository
        extends JpaRepository<PricingPlanCommissionSchemeRefDO, String> {

    List<PricingPlanCommissionSchemeRefDO> findByPlanIdOrderBySortOrderAsc(String planId);

    void deleteByPlanId(String planId);
}
