package com.titanium.product.pricing.aggregate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.common.enums.PricingCalculationStatus;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.pricing.calculation.CalculationLine;
import com.titanium.product.valueobject.pricing.calculation.CalculationTotals;
import com.titanium.product.valueobject.pricing.premium.PremiumAdjustment;
import com.titanium.product.valueobject.pricing.premium.PremiumCalculationEvidence;

import lombok.Getter;

/**
 * Product 不可变确认计算事实。
 */
@Getter
public final class PremiumCalculation {

    private final String calculationId;
    private final String calculationRequestId;
    private final String bizNo;
    private final PricingCalculationPurpose purpose;
    private final PricingCalculationStatus status;
    private final String tenantId;
    private final String productId;
    private final LocalDateTime businessTime;
    private final String currency;
    private final BigDecimal standardPremium;
    private final BigDecimal totalPremium;
    private final BigDecimal installmentAmount;
    private final int periods;
    private final List<PremiumAdjustment> adjustments;
    private final CalculationTotals calculationTotals;
    private final List<CalculationLine> calculationLines;
    private final PremiumCalculationEvidence evidence;
    private final Map<String, Object> requestSnapshot;
    private final String requestHash;
    private final String inputHash;
    private final String resultHash;
    private final LocalDateTime createdAt;

    private PremiumCalculation(
            String calculationId,
            String calculationRequestId,
            String bizNo,
            PricingCalculationPurpose purpose,
            PricingCalculationStatus status,
            String tenantId,
            String productId,
            LocalDateTime businessTime,
            String currency,
            BigDecimal standardPremium,
            BigDecimal totalPremium,
            BigDecimal installmentAmount,
            int periods,
            List<PremiumAdjustment> adjustments,
            CalculationTotals calculationTotals,
            List<CalculationLine> calculationLines,
            PremiumCalculationEvidence evidence,
            Map<String, Object> requestSnapshot,
            String requestHash,
            String inputHash,
            String resultHash,
            LocalDateTime createdAt) {
        this.calculationId = requireText(calculationId, "计算ID");
        this.calculationRequestId = requireText(calculationRequestId, "计算请求ID");
        this.bizNo = requireText(bizNo, "业务号");
        this.purpose = Objects.requireNonNull(purpose, "计算用途不能为空");
        this.status = Objects.requireNonNull(status, "计算状态不能为空");
        this.tenantId = requireText(tenantId, "租户ID");
        this.productId = requireText(productId, "产品ID");
        this.businessTime = Objects.requireNonNull(businessTime, "业务时点不能为空");
        this.currency = requireText(currency, "币种").toUpperCase(Locale.ROOT);
        this.standardPremium = requireAmount(standardPremium, "标准保费");
        this.totalPremium = requireAmount(totalPremium, "最终保费");
        this.installmentAmount = requireAmount(installmentAmount, "分期金额");
        if (periods <= 0) {
            throw invalid("缴费期数必须大于0");
        }
        this.periods = periods;
        this.adjustments = adjustments == null ? List.of() : List.copyOf(adjustments);
        this.calculationTotals = Objects.requireNonNull(calculationTotals, "费用汇总不能为空");
        this.calculationLines = calculationLines == null ? List.of() : List.copyOf(calculationLines);
        if (this.calculationTotals.customerPayable().compareTo(this.totalPremium) != 0) {
            throw invalid("费用汇总客户应付必须等于最终保费");
        }
        this.evidence = Objects.requireNonNull(evidence, "版本证据不能为空");
        this.requestSnapshot = requestSnapshot == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(requestSnapshot));
        this.requestHash = requireHash(requestHash, "请求hash");
        this.inputHash = requireHash(inputHash, "输入hash");
        this.resultHash = requireHash(resultHash, "结果hash");
        this.createdAt = Objects.requireNonNull(createdAt, "创建时间不能为空");
    }

    public static PremiumCalculation confirm(
            String calculationId,
            String calculationRequestId,
            String bizNo,
            PricingCalculationPurpose purpose,
            String tenantId,
            String productId,
            LocalDateTime businessTime,
            String currency,
            BigDecimal standardPremium,
            BigDecimal totalPremium,
            BigDecimal installmentAmount,
            int periods,
            List<PremiumAdjustment> adjustments,
            PremiumCalculationEvidence evidence,
            Map<String, Object> requestSnapshot,
            String requestHash,
            String inputHash,
            String resultHash,
            LocalDateTime createdAt) {
        return confirm(
                calculationId, calculationRequestId, bizNo, purpose, tenantId, productId, businessTime,
                currency, standardPremium, totalPremium, installmentAmount, periods, adjustments,
                CalculationTotals.customerPremium(totalPremium), List.of(), evidence, requestSnapshot,
                requestHash, inputHash, resultHash, createdAt);
    }

    public static PremiumCalculation confirm(
            String calculationId,
            String calculationRequestId,
            String bizNo,
            PricingCalculationPurpose purpose,
            String tenantId,
            String productId,
            LocalDateTime businessTime,
            String currency,
            BigDecimal standardPremium,
            BigDecimal totalPremium,
            BigDecimal installmentAmount,
            int periods,
            List<PremiumAdjustment> adjustments,
            CalculationTotals calculationTotals,
            List<CalculationLine> calculationLines,
            PremiumCalculationEvidence evidence,
            Map<String, Object> requestSnapshot,
            String requestHash,
            String inputHash,
            String resultHash,
            LocalDateTime createdAt) {
        return new PremiumCalculation(
                calculationId, calculationRequestId, bizNo, purpose, PricingCalculationStatus.CONFIRMED,
                tenantId, productId, businessTime, currency, standardPremium, totalPremium,
                installmentAmount, periods, adjustments, calculationTotals, calculationLines, evidence, requestSnapshot,
                requestHash, inputHash, resultHash, createdAt);
    }

    public static PremiumCalculation restore(
            String calculationId,
            String calculationRequestId,
            String bizNo,
            PricingCalculationPurpose purpose,
            PricingCalculationStatus status,
            String tenantId,
            String productId,
            LocalDateTime businessTime,
            String currency,
            BigDecimal standardPremium,
            BigDecimal totalPremium,
            BigDecimal installmentAmount,
            int periods,
            List<PremiumAdjustment> adjustments,
            PremiumCalculationEvidence evidence,
            Map<String, Object> requestSnapshot,
            String requestHash,
            String inputHash,
            String resultHash,
            LocalDateTime createdAt) {
        return restore(
                calculationId, calculationRequestId, bizNo, purpose, status, tenantId, productId,
                businessTime, currency, standardPremium, totalPremium, installmentAmount, periods,
                adjustments, CalculationTotals.customerPremium(totalPremium), List.of(), evidence,
                requestSnapshot, requestHash, inputHash, resultHash, createdAt);
    }

    public static PremiumCalculation restore(
            String calculationId,
            String calculationRequestId,
            String bizNo,
            PricingCalculationPurpose purpose,
            PricingCalculationStatus status,
            String tenantId,
            String productId,
            LocalDateTime businessTime,
            String currency,
            BigDecimal standardPremium,
            BigDecimal totalPremium,
            BigDecimal installmentAmount,
            int periods,
            List<PremiumAdjustment> adjustments,
            CalculationTotals calculationTotals,
            List<CalculationLine> calculationLines,
            PremiumCalculationEvidence evidence,
            Map<String, Object> requestSnapshot,
            String requestHash,
            String inputHash,
            String resultHash,
            LocalDateTime createdAt) {
        return new PremiumCalculation(
                calculationId, calculationRequestId, bizNo, purpose, status, tenantId, productId,
                businessTime, currency, standardPremium, totalPremium, installmentAmount, periods,
                adjustments, calculationTotals, calculationLines, evidence, requestSnapshot,
                requestHash, inputHash, resultHash, createdAt);
    }

    /** 同一幂等键只能对应完全相同的业务请求。 */
    public void assertSameRequest(String candidateRequestHash) {
        if (!requestHash.equals(candidateRequestHash)) {
            throw new PricingDomainException(
                    ProductErrorCode.PRICING_IDEMPOTENCY_CONFLICT,
                    "calculationRequestId 已被不同参数使用: " + calculationRequestId);
        }
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

    private static BigDecimal requireAmount(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw invalid(field + "不能为负数");
        }
        return value;
    }

    private static PricingDomainException invalid(String detail) {
        return new PricingDomainException(ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED, detail);
    }
}
