package com.titanium.product.infrastructure.pricing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.titanium.product.infrastructure.pricing.entity.PricingTestCaseEntity;

/**
 * 定价测试用例 JPA 仓储。
 */
public interface PricingTestCaseJpaRepository extends JpaRepository<PricingTestCaseEntity, String> {

    List<PricingTestCaseEntity> findByPlanIdAndTenantIdOrderByCaseCodeAsc(String planId, String tenantId);

    void deleteByPlanIdAndTenantId(String planId, String tenantId);
}
