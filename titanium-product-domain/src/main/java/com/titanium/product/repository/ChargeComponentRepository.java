package com.titanium.product.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.titanium.product.aggregate.ChargeComponentDefinition;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;

/**
 * 费用项目录仓储端口。
 */
public interface ChargeComponentRepository {

    boolean existsByBusinessKey(String tenantId, String productId, String componentCode, String componentVersion);

    Optional<ChargeComponentDefinition> findById(String tenantId, String productId, String componentId);

    Optional<ChargeComponentDefinition> findPublished(
            String tenantId,
            String productId,
            String componentCode,
            String componentVersion,
            LocalDateTime businessTime);

    List<ChargeComponentDefinition> findAll(
            String tenantId, String productId, ActuarialDefinitionStatus status);

    void save(ChargeComponentDefinition component);
}
