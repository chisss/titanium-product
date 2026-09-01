package com.titanium.product.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.pricing.aggregate.DynamicFactorDefinition;

/**
 * 动态因子仓储端口。
 */
public interface DynamicFactorRepository {

    boolean existsByBusinessKey(String tenantId, String productId, String factorCode, String factorVersion);

    Optional<DynamicFactorDefinition> findById(String tenantId, String productId, String factorId);

    Optional<DynamicFactorDefinition> findPublished(
            String tenantId, String productId, String factorCode, String factorVersion, LocalDateTime businessTime);

    List<DynamicFactorDefinition> findAll(
            String tenantId, String productId, ActuarialDefinitionStatus status);

    void save(DynamicFactorDefinition factor);
}
