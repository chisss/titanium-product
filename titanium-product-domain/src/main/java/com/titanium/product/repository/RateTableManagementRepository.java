package com.titanium.product.repository;

import java.util.List;
import java.util.Optional;

import com.titanium.product.aggregate.RateTableDefinition;
import com.titanium.product.common.enums.RateTableStatus;

/**
 * Product 费率表管理端口。
 */
public interface RateTableManagementRepository {

    boolean existsByBusinessKey(String tenantId, String productId, String tableCode, String tableVersion);

    Optional<RateTableDefinition> findById(String tenantId, String productId, String tableId);

    List<RateTableDefinition> findAll(String tenantId, String productId, RateTableStatus status);

    void save(RateTableDefinition rateTable);
}
