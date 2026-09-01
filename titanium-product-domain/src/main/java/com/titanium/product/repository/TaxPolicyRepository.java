package com.titanium.product.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.pricing.aggregate.TaxPolicyDefinition;

/**
 * 税费策略仓储端口。
 */
public interface TaxPolicyRepository {

    boolean existsByBusinessKey(String tenantId, String productId, String policyCode, String policyVersion);

    Optional<TaxPolicyDefinition> findById(String tenantId, String productId, String policyId);

    Optional<TaxPolicyDefinition> findPublished(
            String tenantId, String productId, String policyCode, String policyVersion, LocalDateTime businessTime);

    List<TaxPolicyDefinition> findAll(String tenantId, String productId, ActuarialDefinitionStatus status);

    void save(TaxPolicyDefinition policy);
}
