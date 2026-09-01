package com.titanium.product.infrastructure.pricing.repository.premium;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.pricing.entity.premium.TaxBaseItemDO;

public interface TaxBaseItemJpaRepository extends JpaRepository<TaxBaseItemDO, String> {

    List<TaxBaseItemDO> findByPolicyIdOrderBySortOrderAsc(String policyId);
}
