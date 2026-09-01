package com.titanium.product.infrastructure.pricing.adapter.premium;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.product.infrastructure.pricing.entity.premium.RateTableDO;
import com.titanium.product.infrastructure.pricing.entity.premium.RateTableRowDO;
import com.titanium.product.infrastructure.pricing.repository.premium.RateTableJpaRepository;
import com.titanium.product.infrastructure.pricing.repository.premium.RateTableRowJpaRepository;
import com.titanium.product.repository.RateTableSnapshotRepository;
import com.titanium.product.valueobject.rate.RateTableCriteria;
import com.titanium.product.valueobject.rate.RateTableRow;
import com.titanium.product.valueobject.rate.RateTableSnapshot;

import lombok.RequiredArgsConstructor;

/**
 * Product 费率表快照仓储的 JPA 适配器。
 */
@Repository
@RequiredArgsConstructor
public class JpaRateTableSnapshotRepository implements RateTableSnapshotRepository {

    private final RateTableJpaRepository rateTableJpaRepository;
    private final RateTableRowJpaRepository rateTableRowJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<RateTableSnapshot> findEffectiveSnapshot(
            String tenantId,
            String productId,
            String tableCode,
            String tableVersion,
            LocalDateTime businessTime,
            RateTableCriteria criteria) {
        return rateTableJpaRepository.findEffectiveTable(
                        tenantId, productId, tableCode, tableVersion, businessTime)
                .map(table -> toSnapshot(table, criteria));
    }

    private RateTableSnapshot toSnapshot(RateTableDO table, RateTableCriteria criteria) {
        List<RateTableRow> rows = rateTableRowJpaRepository.findCandidateRows(
                        table.getTableId(), table.getTenantId(), criteria.age(), criteria.gender(),
                        criteria.paymentTermYears(), criteria.coverageTermYears())
                .stream()
                .map(this::toDomainRow)
                .toList();
        return new RateTableSnapshot(
                table.getTableId(), table.getProductId(), table.getTableCode(), table.getTableVersion(),
                table.getRateUnit(), table.getCurrency(), table.getEffectiveFrom(), table.getEffectiveTo(),
                table.getContentHash(), rows);
    }

    private RateTableRow toDomainRow(RateTableRowDO row) {
        return new RateTableRow(
                row.getRowId(), row.getAgeFrom(), row.getAgeToExclusive(), row.getGender(),
                row.getPaymentTermYears(), row.getCoverageTermYears(), row.getRate(),
                row.getMinimumPremium(), row.getMaximumPremium());
    }
}
