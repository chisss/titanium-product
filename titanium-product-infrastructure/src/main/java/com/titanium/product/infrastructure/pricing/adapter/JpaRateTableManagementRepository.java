package com.titanium.product.infrastructure.pricing.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson2.JSON;

import com.titanium.product.aggregate.RateTableDefinition;
import com.titanium.product.common.enums.RateTableStatus;
import com.titanium.product.infrastructure.mapper.RateTablePersistenceMapper;
import com.titanium.product.infrastructure.pricing.entity.RateTableDO;
import com.titanium.product.infrastructure.pricing.entity.RateTableRowDO;
import com.titanium.product.infrastructure.pricing.repository.RateTableJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.RateTableRowJpaRepository;
import com.titanium.product.repository.RateTableManagementRepository;
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
    private final RateTablePersistenceMapper persistenceMapper;

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
        List<RateTableDO> dataObjects = status == null
                ? rateTableJpaRepository.findByTenantIdAndProductIdOrderByCreateTimeDesc(tenantId, productId)
                : rateTableJpaRepository.findByTenantIdAndProductIdAndStatusOrderByCreateTimeDesc(
                        tenantId, productId, status);
        return dataObjects.stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void save(RateTableDefinition rateTable) {
        rateTableJpaRepository.save(persistenceMapper.toDO(rateTable));
        if (rateTable.status() == RateTableStatus.DRAFT) {
            rateTableRowJpaRepository.deleteByTableIdAndTenantId(rateTable.tableId(), rateTable.tenantId());
            rateTableRowJpaRepository.flush();
            rateTableRowJpaRepository.saveAll(rateTable.rows().stream()
                    .map(row -> persistenceMapper.toDO(rateTable, row))
                    .toList());
        }
    }

    private RateTableDefinition toDomain(RateTableDO dataObject) {
        List<RateTableRow> rows = rateTableRowJpaRepository
                .findByTableIdAndTenantIdOrderByCreateTimeAsc(dataObject.getTableId(), dataObject.getTenantId())
                .stream()
                .map(this::toDomainRow)
                .toList();
        List<String> dimensionKeys = JSON.parseArray(dataObject.getDimensionKeysJson(), String.class);
        return RateTableDefinition.restore(
                dataObject.getTableId(), dataObject.getProductId(), dataObject.getTableCode(),
                dataObject.getTableVersion(), dataObject.getStatus(), dataObject.getRateUnit(),
                dataObject.getCurrency(), dataObject.getEffectiveFrom(), dataObject.getEffectiveTo(),
                dimensionKeys, dataObject.getTenantId(), rows, dataObject.getContentHash());
    }

    private RateTableRow toDomainRow(RateTableRowDO dataObject) {
        return new RateTableRow(
                dataObject.getRowId(), dataObject.getAgeFrom(), dataObject.getAgeToExclusive(),
                dataObject.getGender(), dataObject.getPaymentTermYears(), dataObject.getCoverageTermYears(),
                dataObject.getRate(), dataObject.getMinimumPremium(), dataObject.getMaximumPremium());
    }
}
