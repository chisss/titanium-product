package com.titanium.product.infrastructure.pricing.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;

import com.titanium.product.aggregate.RateTableDefinition;
import com.titanium.product.common.enums.RateTableStatus;
import com.titanium.product.infrastructure.pricing.entity.RateTableEntity;
import com.titanium.product.infrastructure.pricing.entity.RateTableRowEntity;
import com.titanium.product.infrastructure.pricing.repository.RateTableJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.RateTableRowJpaRepository;
import com.titanium.product.port.RateTableManagementRepository;
import com.titanium.product.valueobject.RateTableRow;

import lombok.RequiredArgsConstructor;

/**
 * Product 费率表管理端口的 JPA 适配器。
 */
@Repository
@RequiredArgsConstructor
public class JpaRateTableManagementRepository implements RateTableManagementRepository {

    private final RateTableJpaRepository rateTableJpaRepository;
    private final RateTableRowJpaRepository rateTableRowJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean existsByBusinessKey(
            String tenantId, String productId, String tableCode, String tableVersion) {
        return rateTableJpaRepository.existsByTenantIdAndProductIdAndTableCodeAndTableVersion(
                tenantId, productId, tableCode, tableVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RateTableDefinition> findById(String tenantId, String productId, String tableId) {
        return rateTableJpaRepository.findByTableIdAndTenantIdAndProductId(tableId, tenantId, productId)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RateTableDefinition> findAll(String tenantId, String productId, RateTableStatus status) {
        List<RateTableEntity> entities = status == null
                ? rateTableJpaRepository.findByTenantIdAndProductIdOrderByCreateTimeDesc(tenantId, productId)
                : rateTableJpaRepository.findByTenantIdAndProductIdAndStatusOrderByCreateTimeDesc(
                        tenantId, productId, status);
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void save(RateTableDefinition rateTable) {
        rateTableJpaRepository.save(toEntity(rateTable));
        if (rateTable.status() == RateTableStatus.DRAFT) {
            rateTableRowJpaRepository.deleteByTableIdAndTenantId(rateTable.tableId(), rateTable.tenantId());
            rateTableRowJpaRepository.flush();
            rateTableRowJpaRepository.saveAll(rateTable.rows().stream()
                    .map(row -> toEntity(rateTable, row))
                    .toList());
        }
    }

    private RateTableDefinition toDomain(RateTableEntity entity) {
        List<RateTableRow> rows = rateTableRowJpaRepository
                .findByTableIdAndTenantIdOrderByCreateTimeAsc(entity.getTableId(), entity.getTenantId())
                .stream()
                .map(this::toDomainRow)
                .toList();
        List<String> dimensionKeys = JSON.parseArray(entity.getDimensionKeysJson(), String.class);
        return RateTableDefinition.restore(
                entity.getTableId(), entity.getProductId(), entity.getTableCode(), entity.getTableVersion(),
                entity.getStatus(), entity.getRateUnit(), entity.getCurrency(), entity.getEffectiveFrom(),
                entity.getEffectiveTo(), dimensionKeys, entity.getTenantId(), rows, entity.getContentHash());
    }

    private RateTableEntity toEntity(RateTableDefinition rateTable) {
        RateTableEntity entity = new RateTableEntity();
        entity.setTableId(rateTable.tableId());
        entity.setProductId(rateTable.productId());
        entity.setTableCode(rateTable.tableCode());
        entity.setTableVersion(rateTable.tableVersion());
        entity.setStatus(rateTable.status());
        entity.setRateUnit(rateTable.rateUnit());
        entity.setCurrency(rateTable.currency());
        entity.setEffectiveFrom(rateTable.effectiveFrom());
        entity.setEffectiveTo(rateTable.effectiveTo());
        entity.setDimensionKeysJson(JSON.toJSONString(rateTable.dimensionKeys()));
        entity.setContentHash(rateTable.contentHash());
        entity.setRowCount(rateTable.rows().size());
        entity.setTenantId(rateTable.tenantId());
        return entity;
    }

    private RateTableRow toDomainRow(RateTableRowEntity entity) {
        return new RateTableRow(
                entity.getRowId(), entity.getAgeFrom(), entity.getAgeToExclusive(), entity.getGender(),
                entity.getPaymentTermYears(), entity.getCoverageTermYears(), entity.getRate(),
                entity.getMinimumPremium(), entity.getMaximumPremium());
    }

    private RateTableRowEntity toEntity(RateTableDefinition rateTable, RateTableRow row) {
        RateTableRowEntity entity = new RateTableRowEntity();
        entity.setRowId(row.rowId());
        entity.setTableId(rateTable.tableId());
        entity.setDimensionHash(row.dimensionHash());
        entity.setAgeFrom(row.ageFrom());
        entity.setAgeToExclusive(row.ageToExclusive());
        entity.setGender(row.gender());
        entity.setPaymentTermYears(row.paymentTermYears());
        entity.setCoverageTermYears(row.coverageTermYears());
        entity.setRate(row.rate());
        entity.setMinimumPremium(row.minimumPremium());
        entity.setMaximumPremium(row.maximumPremium());
        entity.setTenantId(rateTable.tenantId());
        return entity;
    }
}
