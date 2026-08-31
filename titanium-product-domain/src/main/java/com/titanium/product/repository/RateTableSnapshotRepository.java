package com.titanium.product.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import com.titanium.product.valueobject.RateTableCriteria;
import com.titanium.product.valueobject.RateTableSnapshot;

/**
 * Product 费率表运行时快照仓储端口。
 */
public interface RateTableSnapshotRepository {

    /**
     * 查询指定业务时点的已发布费率表及候选行。
     */
    Optional<RateTableSnapshot> findEffectiveSnapshot(
            String tenantId,
            String productId,
            String tableCode,
            String tableVersion,
            LocalDateTime businessTime,
            RateTableCriteria criteria);
}
