package com.titanium.product.pricing.aggregate.surrender;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.common.enums.SurrenderRefundType;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.pricing.premium.SurrenderValueOutcome;

import lombok.Getter;

/** Product 版本化退保价值策略。 */
@Getter
public final class SurrenderValuePolicy {

    private static final BigDecimal ONE = BigDecimal.ONE;

    private final String policyId;
    private final String productId;
    private final String policyCode;
    private final String policyVersion;
    private final int policyYear;
    private final int coolingOffDays;
    private final BigDecimal cashValueRate;
    private final BigDecimal internalCostRetentionRate;
    private final LocalDateTime effectiveFrom;
    private final LocalDateTime effectiveTo;
    private final String tenantId;
    private ActuarialDefinitionStatus status;
    private String contentHash;

    private SurrenderValuePolicy(
            String policyId,
            String productId,
            String policyCode,
            String policyVersion,
            int policyYear,
            int coolingOffDays,
            BigDecimal cashValueRate,
            BigDecimal internalCostRetentionRate,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId,
            ActuarialDefinitionStatus status,
            String contentHash) {
        this.policyId = requireText(policyId, "退保价值策略ID");
        this.productId = requireText(productId, "产品ID");
        this.policyCode = requireText(policyCode, "退保价值策略编码").toUpperCase(Locale.ROOT);
        this.policyVersion = requireText(policyVersion, "退保价值策略版本");
        if (policyYear < 1 || coolingOffDays < 0) {
            throw invalid("保单年度必须大于0且犹豫期天数不能为负数");
        }
        this.policyYear = policyYear;
        this.coolingOffDays = coolingOffDays;
        this.cashValueRate = requireRate(cashValueRate, "现金价值率");
        this.internalCostRetentionRate = requireRate(internalCostRetentionRate, "内部成本保留率");
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "生效时间不能为空");
        this.effectiveTo = effectiveTo;
        this.tenantId = requireText(tenantId, "租户ID");
        this.status = Objects.requireNonNull(status, "生命周期状态不能为空");
        this.contentHash = contentHash == null ? "" : contentHash;
        if (effectiveTo != null && !effectiveTo.isAfter(effectiveFrom)) {
            throw invalid("失效时间必须晚于生效时间");
        }
    }

    public static SurrenderValuePolicy createDraft(
            String policyId,
            String productId,
            String policyCode,
            String policyVersion,
            int policyYear,
            int coolingOffDays,
            BigDecimal cashValueRate,
            BigDecimal internalCostRetentionRate,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId) {
        return new SurrenderValuePolicy(
                policyId, productId, policyCode, policyVersion, policyYear, coolingOffDays,
                cashValueRate, internalCostRetentionRate, effectiveFrom, effectiveTo, tenantId,
                ActuarialDefinitionStatus.DRAFT, "");
    }

    public static SurrenderValuePolicy restore(
            String policyId,
            String productId,
            String policyCode,
            String policyVersion,
            int policyYear,
            int coolingOffDays,
            BigDecimal cashValueRate,
            BigDecimal internalCostRetentionRate,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId,
            ActuarialDefinitionStatus status,
            String contentHash) {
        return new SurrenderValuePolicy(
                policyId, productId, policyCode, policyVersion, policyYear, coolingOffDays,
                cashValueRate, internalCostRetentionRate, effectiveFrom, effectiveTo, tenantId, status, contentHash);
    }

    public String approve() {
        requireStatus(ActuarialDefinitionStatus.DRAFT, "只有草稿退保价值策略可以审批");
        contentHash = hash(canonicalContent());
        status = ActuarialDefinitionStatus.APPROVED;
        return contentHash;
    }

    public void publish() {
        requireStatus(ActuarialDefinitionStatus.APPROVED, "只有已审批退保价值策略可以发布");
        status = ActuarialDefinitionStatus.PUBLISHED;
    }

    public void retire() {
        requireStatus(ActuarialDefinitionStatus.PUBLISHED, "只有已发布退保价值策略可以退役");
        status = ActuarialDefinitionStatus.RETIRED;
    }

    public boolean isEffectiveAt(LocalDateTime businessTime) {
        return status == ActuarialDefinitionStatus.PUBLISHED && businessTime != null
                && !businessTime.isBefore(effectiveFrom)
                && (effectiveTo == null || businessTime.isBefore(effectiveTo));
    }

    /** 根据已确认客户应付计算退保后应退和保留金额。 */
    public SurrenderValueOutcome calculate(
            BigDecimal originalCustomerPayable,
            LocalDate policyEffectiveDate,
            LocalDate surrenderDate,
            int scale,
            RoundingMode roundingMode) {
        if (originalCustomerPayable == null || originalCustomerPayable.signum() < 0
                || policyEffectiveDate == null || surrenderDate == null || surrenderDate.isBefore(policyEffectiveDate)
                || scale < 0 || roundingMode == null) {
            throw invalid("退保价值计算输入不完整或不合法");
        }
        boolean withinCoolingOff = !surrenderDate.isAfter(policyEffectiveDate.plusDays(coolingOffDays));
        BigDecimal refundRate = withinCoolingOff ? ONE : cashValueRate;
        BigDecimal refundAmount = originalCustomerPayable.multiply(refundRate).setScale(scale, roundingMode);
        return new SurrenderValueOutcome(
                withinCoolingOff ? SurrenderRefundType.COOLING_OFF_FULL_REFUND : SurrenderRefundType.CASH_VALUE,
                withinCoolingOff, refundRate, refundAmount,
                originalCustomerPayable.subtract(refundAmount),
                withinCoolingOff ? BigDecimal.ZERO : internalCostRetentionRate);
    }

    private String canonicalContent() {
        return String.join("|", productId, policyCode, policyVersion, Integer.toString(policyYear),
                Integer.toString(coolingOffDays), decimal(cashValueRate), decimal(internalCostRetentionRate),
                effectiveFrom.toString(), effectiveTo == null ? "*" : effectiveTo.toString(), tenantId);
    }

    private void requireStatus(ActuarialDefinitionStatus expected, String message) {
        if (status != expected) {
            throw invalid(message);
        }
    }

    private static BigDecimal requireRate(BigDecimal value, String field) {
        if (value == null || value.signum() < 0 || value.compareTo(ONE) > 0) {
            throw invalid(field + "必须在0到1之间");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalid(field + "不能为空");
        }
        return value.trim();
    }

    private static String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("运行环境不支持SHA-256", exception);
        }
    }

    private static PricingDomainException invalid(String detail) {
        return new PricingDomainException(ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED, detail);
    }
}
