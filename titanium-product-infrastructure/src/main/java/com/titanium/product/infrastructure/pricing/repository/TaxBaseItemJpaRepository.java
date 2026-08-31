package com.titanium.product.infrastructure.pricing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.pricing.entity.TaxBaseItemDO;

public interface TaxBaseItemJpaRepository extends JpaRepository<TaxBaseItemDO, String> {

    List<TaxBaseItemDO> findByPolicyIdOrderBySortOrderAsc(String policyId);
}
