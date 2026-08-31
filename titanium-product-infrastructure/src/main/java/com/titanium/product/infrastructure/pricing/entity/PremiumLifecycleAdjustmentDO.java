package com.titanium.product.infrastructure.pricing.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.titanium.product.common.enums.PremiumBalanceDirection;
import com.titanium.product.common.enums.PremiumLifecycleType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Product 生命周期费用差额事实实体。
 */
@Entity
@Table(name = "t_product_premium_lifecycle_adjustment")
@Getter
@Setter
public class PremiumLifecycleAdjustmentDO {

    @Id
    @Column(name = "adjustment_id", nullable = false, length = 36)
    private String adjustmentId;
    @Column(name = "adjustment_request_id", nullable = false, length = 64)
    private String adjustmentRequestId;
    @Column(name = "reversal_of_adjustment_id", length = 36)
    private String reversalOfAdjustmentId;
    @Column(name = "biz_no", nullable = false, length = 128)
    private String bizNo;
    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_type", nullable = false, length = 24)
    private PremiumLifecycleType lifecycleType;
    @Column(name = "tenant_id", nullable = false, length = 32)
    private String tenantId;
    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;
    @Column(name = "original_calculation_id", nullable = false, length = 36)
    private String originalCalculationId;
    @Column(name = "original_result_hash", nullable = false, length = 64)
    private String originalResultHash;
    @Column(name = "replacement_calculation_id", nullable = false, length = 36)
    private String replacementCalculationId;
    @Column(name = "replacement_result_hash", nullable = false, length = 64)
    private String replacementResultHash;
    @Column(name = "business_time", nullable = false)
    private LocalDateTime businessTime;
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 12)
    private PremiumBalanceDirection direction;
    @Column(name = "customer_amount", nullable = false, precision = 20, scale = 8)
    private BigDecimal customerAmount;
    @Enumerated(EnumType.STRING)
    @Column(name = "tax_direction", nullable = false, length = 12)
    private PremiumBalanceDirection taxDirection;
    @Column(name = "tax_amount", nullable = false, precision = 20, scale = 8)
    private BigDecimal taxAmount;
    @Enumerated(EnumType.STRING)
    @Column(name = "internal_cost_direction", nullable = false, length = 12)
    private PremiumBalanceDirection internalCostDirection;
    @Column(name = "internal_cost_amount", nullable = false, precision = 20, scale = 8)
    private BigDecimal internalCostAmount;
    @Column(name = "reason", nullable = false, length = 500)
    private String reason;
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;
    @Column(name = "result_hash", nullable = false, length = 64)
    private String resultHash;
    @Column(name = "create_time", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
