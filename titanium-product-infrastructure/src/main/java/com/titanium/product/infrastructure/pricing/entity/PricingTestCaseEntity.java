package com.titanium.product.infrastructure.pricing.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 定价方案发布回归测试用例实体。
 */
@Entity
@Table(name = "t_product_pricing_test_case")
@Getter
@Setter
public class PricingTestCaseEntity {

    @Id
    @Column(name = "case_id", nullable = false, length = 36)
    private String caseId;

    @Column(name = "plan_id", nullable = false, length = 36)
    private String planId;

    @Column(name = "case_code", nullable = false, length = 64)
    private String caseCode;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "business_time", nullable = false)
    private LocalDateTime businessTime;

    @Column(name = "sum_insured", nullable = false, precision = 20, scale = 2)
    private BigDecimal sumInsured;

    @Column(name = "age", nullable = false)
    private int age;

    @Column(name = "gender", nullable = false, length = 8)
    private String gender;

    @Column(name = "payment_term_years", nullable = false)
    private int paymentTermYears;

    @Column(name = "coverage_term_years", nullable = false)
    private int coverageTermYears;

    @Column(name = "payment_periods", nullable = false)
    private int paymentPeriods;

    @Column(name = "request_snapshot_json", nullable = false, columnDefinition = "LONGTEXT")
    private String requestSnapshotJson;

    @Column(name = "expected_premium", nullable = false, precision = 20, scale = 8)
    private BigDecimal expectedPremium;

    @Column(name = "tolerance", nullable = false, precision = 20, scale = 8)
    private BigDecimal tolerance;

    @Column(name = "tenant_id", nullable = false, length = 32)
    private String tenantId;

    @Column(name = "create_time", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createTime;
}
