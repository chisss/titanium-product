package com.titanium.product.pricing.aggregate.loan;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.exception.PricingDomainException;

import lombok.Getter;

/**
 * Product 版本化保单贷款策略（g04-03）。
 * <p>
 * 承载保单贷款（以保单现金价值为质押向保险公司借款）的定价配置：可贷比例与贷款年利率，
 * 生命周期与 {@code SurrenderValuePolicy} 同构（DRAFT → APPROVED → PUBLISHED → RETIRED + contentHash）。
 * 放款时由保全域编排器读取当前已发布策略并将比例/利率<b>冻结</b>进贷款账户，
 * 后续策略调整不回溯存量贷款。
 * </p>
 */
@Getter
public final class PolicyLoanPolicy {

    private static final BigDecimal ONE = BigDecimal.ONE;

    private final String policyId;
    private final String productId;
    private final String policyCode;
    private final String policyVersion;
    private final BigDecimal loanableRatio;
    private final BigDecimal interestRate;
    private final LocalDateTime effectiveFrom;
    private final LocalDateTime effectiveTo;
    private final String tenantId;
    private ActuarialDefinitionStatus status;
    private String contentHash;

    private PolicyLoanPolicy(
            String policyId,
            String productId,
            String policyCode,
            String policyVersion,
            BigDecimal loanableRatio,
            BigDecimal interestRate,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId,
            ActuarialDefinitionStatus status,
            String contentHash) {
        this.policyId = requireText(policyId, "贷款策略ID");
        this.productId = requireText(productId, "产品ID");
        this.policyCode = requireText(policyCode, "贷款策略编码").toUpperCase(Locale.ROOT);
        this.policyVersion = requireText(policyVersion, "贷款策略版本");
        this.loanableRatio = requireRate(loanableRatio, "可贷比例");
        this.interestRate = requireRate(interestRate, "贷款年利率");
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "生效时间不能为空");
        this.effectiveTo = effectiveTo;
        this.tenantId = requireText(tenantId, "租户ID");
        this.status = Objects.requireNonNull(status, "生命周期状态不能为空");
        this.contentHash = contentHash == null ? "" : contentHash;
        if (effectiveTo != null && !effectiveTo.isAfter(effectiveFrom)) {
            throw invalid("失效时间必须晚于生效时间");
        }
    }

    public static PolicyLoanPolicy createDraft(
            String policyId,
            String productId,
            String policyCode,
            String policyVersion,
            BigDecimal loanableRatio,
            BigDecimal interestRate,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId) {
        return new PolicyLoanPolicy(
                policyId, productId, policyCode, policyVersion, loanableRatio, interestRate,
                effectiveFrom, effectiveTo, tenantId, ActuarialDefinitionStatus.DRAFT, "");
    }

    public static PolicyLoanPolicy restore(
            String policyId,
            String productId,
            String policyCode,
            String policyVersion,
            BigDecimal loanableRatio,
            BigDecimal interestRate,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId,
            ActuarialDefinitionStatus status,
            String contentHash) {
        return new PolicyLoanPolicy(
                policyId, productId, policyCode, policyVersion, loanableRatio, interestRate,
                effectiveFrom, effectiveTo, tenantId, status, contentHash);
    }

    public String approve() {
        requireStatus(ActuarialDefinitionStatus.DRAFT, "只有草稿贷款策略可以审批");
        contentHash = hash(canonicalContent());
        status = ActuarialDefinitionStatus.APPROVED;
        return contentHash;
    }

    public void publish() {
        requireStatus(ActuarialDefinitionStatus.APPROVED, "只有已审批贷款策略可以发布");
        status = ActuarialDefinitionStatus.PUBLISHED;
    }

    public void retire() {
        requireStatus(ActuarialDefinitionStatus.PUBLISHED, "只有已发布贷款策略可以退役");
        status = ActuarialDefinitionStatus.RETIRED;
    }

    /** 业务时点是否适用（已发布且落入生效区间）。 */
    public boolean isEffectiveAt(LocalDateTime businessTime) {
        return status == ActuarialDefinitionStatus.PUBLISHED && businessTime != null
                && !businessTime.isBefore(effectiveFrom)
                && (effectiveTo == null || businessTime.isBefore(effectiveTo));
    }

    private String canonicalContent() {
        return String.join("|", productId, policyCode, policyVersion, decimal(loanableRatio),
                decimal(interestRate), effectiveFrom.toString(),
                effectiveTo == null ? "*" : effectiveTo.toString(), tenantId);
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
