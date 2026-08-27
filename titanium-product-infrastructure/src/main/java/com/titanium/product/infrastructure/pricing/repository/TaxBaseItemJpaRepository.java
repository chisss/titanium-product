package com.titanium.product.infrastructure.pricing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.pricing.entity.TaxBaseItemEntity;

public interface TaxBaseItemJpaRepository extends JpaRepository<TaxBaseItemEntity, String> {

    List<TaxBaseItemEntity> findByPolicyIdOrderBySortOrderAsc(String policyId);
}
