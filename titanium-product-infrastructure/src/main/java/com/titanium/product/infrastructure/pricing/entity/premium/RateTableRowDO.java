package com.titanium.product.infrastructure.pricing.entity.premium;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Product 不可变费率表行实体。
 */
@Entity
@Table(name = "t_product_rate_table_row")
@Getter
@Setter
public class RateTableRowDO {

    @Id
    @Column(name = "row_id", nullable = false, length = 36)
    private String rowId;

    @Column(name = "table_id", nullable = false, length = 36)
    private String tableId;

    @Column(name = "dimension_hash", nullable = false, length = 64)
    private String dimensionHash;

    @Column(name = "age_from")
    private Integer ageFrom;

    @Column(name = "age_to_exclusive")
    private Integer ageToExclusive;

    @Column(name = "gender", length = 8)
    private String gender;

    @Column(name = "payment_term_years")
    private Integer paymentTermYears;

    @Column(name = "coverage_term_years")
    private Integer coverageTermYears;

    @Column(name = "rate", nullable = false, precision = 18, scale = 8)
    private BigDecimal rate;

    @Column(name = "minimum_premium", precision = 18, scale = 2)
    private BigDecimal minimumPremium;

    @Column(name = "maximum_premium", precision = 18, scale = 2)
    private BigDecimal maximumPremium;

    @Column(name = "tenant_id", nullable = false, length = 32)
    private String tenantId;

    @Column(name = "create_time", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createTime;
}
