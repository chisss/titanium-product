package com.titanium.product.infrastructure.pricing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.titanium.product.infrastructure.pricing.entity.RateTableRowDO;

/**
 * 费率表行 JPA 仓储。
 */
public interface RateTableRowJpaRepository extends JpaRepository<RateTableRowDO, String> {

    List<RateTableRowDO> findByTableIdAndTenantIdOrderByCreateTimeAsc(String tableId, String tenantId);

    void deleteByTableIdAndTenantId(String tableId, String tenantId);

    @Query("""
            select rateRow from RateTableRowDO rateRow
             where rateRow.tableId = :tableId
               and rateRow.tenantId = :tenantId
               and (rateRow.ageFrom is null or rateRow.ageFrom <= :age)
               and (rateRow.ageToExclusive is null or :age < rateRow.ageToExclusive)
               and (rateRow.gender is null or upper(rateRow.gender) = 'ALL'
                    or upper(rateRow.gender) = upper(:gender))
               and (rateRow.paymentTermYears is null or rateRow.paymentTermYears = :paymentTermYears)
               and (rateRow.coverageTermYears is null or rateRow.coverageTermYears = :coverageTermYears)
            """)
    List<RateTableRowDO> findCandidateRows(
            @Param("tableId") String tableId,
            @Param("tenantId") String tenantId,
            @Param("age") int age,
            @Param("gender") String gender,
            @Param("paymentTermYears") int paymentTermYears,
            @Param("coverageTermYears") int coverageTermYears);
}
