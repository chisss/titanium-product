package com.titanium.product.aggregate.lifecycle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.PremiumBalanceDirection;
import com.titanium.product.common.enums.PremiumLifecycleType;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.pricing.lifecycle.PremiumLifecycleDifference;
import com.titanium.product.valueobject.pricing.lifecycle.PremiumLifecycleDifferenceLine;

import lombok.Getter;

/**
 * Product 保存的不可变保单生命周期费用差额事实。
 */
@Getter
public final class PremiumLifecycleAdjustment {

    private final String adjustmentId;
    private final String adjustmentRequestId;
    private final String reversalOfAdjustmentId;
    private final String bizNo;
    private final PremiumLifecycleType lifecycleType;
    private final String tenantId;
    private final String productId;
    private final String originalCalculationId;
    private final String originalResultHash;
    private final String replacementCalculationId;
    private final String replacementResultHash;
    private final LocalDateTime businessTime;
    private final String currency;
    private final PremiumBalanceDirection direction;
    private final BigDecimal customerAmount;
    private final PremiumBalanceDirection taxDirection;
    private final BigDecimal taxAmount;
    private final PremiumBalanceDirection internalCostDirection;
    private final BigDecimal internalCostAmount;
    private final List<PremiumLifecycleDifferenceLine> lines;
    private final String reason;
    private final String requestHash;
    private final String resultHash;
    private final LocalDateTime createdAt;

    private PremiumLifecycleAdjustment(
            String adjustmentId,
            String adjustmentRequestId,
            String reversalOfAdjustmentId,
            String bizNo,
            PremiumLifecycleType lifecycleType,
            String tenantId,
            String productId,
            String originalCalculationId,
            String originalResultHash,
            String replacementCalculationId,
            String replacementResultHash,
            LocalDateTime businessTime,
            String currency,
            PremiumLifecycleDifference difference,
            String reason,
            String requestHash,
            String resultHash,
            LocalDateTime createdAt) {
        this.adjustmentId = requireText(adjustmentId, "差额事实ID");
        this.adjustmentRequestId = requireText(adjustmentRequestId, "差额请求ID");
        this.reversalOfAdjustmentId = reversalOfAdjustmentId == null || reversalOfAdjustmentId.isBlank()
                ? null : reversalOfAdjustmentId.trim();
        this.bizNo = requireText(bizNo, "业务号");
        this.lifecycleType = Objects.requireNonNull(lifecycleType, "生命周期类型不能为空");
        this.tenantId = requireText(tenantId, "租户ID");
        this.productId = requireText(productId, "产品ID");
        this.originalCalculationId = requireText(originalCalculationId, "原计算ID");
        this.originalResultHash = requireHash(originalResultHash, "原计算结果hash");
        this.replacementCalculationId = requireText(replacementCalculationId, "替代计算ID");
        this.replacementResultHash = requireHash(replacementResultHash, "替代计算结果hash");
        this.businessTime = Objects.requireNonNull(businessTime, "业务时点不能为空");
        this.currency = requireText(currency, "币种").toUpperCase(Locale.ROOT);
        PremiumLifecycleDifference safeDifference = Objects.requireNonNull(difference, "差额不能为空");
        this.direction = safeDifference.direction();
        this.customerAmount = safeDifference.customerAmount();
        this.taxDirection = safeDifference.taxDirection();
        this.taxAmount = safeDifference.taxAmount();
        this.internalCostDirection = safeDifference.internalCostDirection();
        this.internalCostAmount = safeDifference.internalCostAmount();
        this.lines = safeDifference.lines();
        this.reason = requireText(reason, "变更原因");
        this.requestHash = requireHash(requestHash, "请求hash");
        this.resultHash = requireHash(resultHash, "结果hash");
        this.createdAt = Objects.requireNonNull(createdAt, "创建时间不能为空");
    }

    public static PremiumLifecycleAdjustment confirm(
            String adjustmentId,
            String adjustmentRequestId,
            String bizNo,
            PremiumLifecycleType lifecycleType,
            String tenantId,
            String productId,
            String originalCalculationId,
            String originalResultHash,
            String replacementCalculationId,
            String replacementResultHash,
            LocalDateTime businessTime,
            String currency,
            PremiumLifecycleDifference difference,
            String reason,
            String requestHash,
            String resultHash,
            LocalDateTime createdAt) {
        return new PremiumLifecycleAdjustment(
                adjustmentId, adjustmentRequestId, null, bizNo, lifecycleType, tenantId, productId,
                originalCalculationId, originalResultHash, replacementCalculationId, replacementResultHash,
                businessTime, currency, difference, reason, requestHash, resultHash, createdAt);
    }

    public static PremiumLifecycleAdjustment confirmReversal(
            String adjustmentId,
            String adjustmentRequestId,
            String sourceAdjustmentId,
            String bizNo,
            PremiumLifecycleType lifecycleType,
            String tenantId,
            String productId,
            String originalCalculationId,
            String originalResultHash,
            String replacementCalculationId,
            String replacementResultHash,
            LocalDateTime businessTime,
            String currency,
            PremiumLifecycleDifference difference,
            String reason,
            String requestHash,
            String resultHash,
            LocalDateTime createdAt) {
        return new PremiumLifecycleAdjustment(
                adjustmentId, adjustmentRequestId, sourceAdjustmentId, bizNo, lifecycleType, tenantId, productId,
                originalCalculationId, originalResultHash, replacementCalculationId, replacementResultHash,
                businessTime, currency, difference, reason, requestHash, resultHash, createdAt);
    }

    public void assertSameRequest(String candidateRequestHash) {
        if (!requestHash.equals(candidateRequestHash)) {
            throw new PricingDomainException(
                    ProductErrorCode.PRICING_IDEMPOTENCY_CONFLICT,
                    "adjustmentRequestId 已被不同参数使用: " + adjustmentRequestId);
        }
    }

    public String getReversalOfAdjustmentId() {
        return reversalOfAdjustmentId;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalid(field + "不能为空");
        }
        return value.trim();
    }

    private static String requireHash(String value, String field) {
        String hash = requireText(value, field);
        if (hash.length() != 64) {
            throw invalid(field + "必须为SHA-256");
        }
        return hash;
    }

    private static PricingDomainException invalid(String detail) {
        return new PricingDomainException(ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED, detail);
    }
}
