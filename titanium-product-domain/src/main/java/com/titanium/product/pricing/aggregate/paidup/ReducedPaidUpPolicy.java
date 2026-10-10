package com.titanium.product.pricing.aggregate.paidup;

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
 * Product 版本化减额缴清净费率策略（g04-05）。
 * <p>
 * 承载「减额缴清」（投保人停止缴费，以保单当时现金价值一次性趸缴，换取降低后的保额，
 * 合同继续有效）的定价配置：<b>每元保额趸缴净费率</b>。生命周期与
 * {@code SurrenderValuePolicy}/{@code PolicyLoanPolicy} 同构
 * （DRAFT → APPROVED → PUBLISHED → RETIRED + contentHash）。
 * </p>
 * <p>
 * 🔴 <b>公式方向</b>：{@code 新保额 = 现金价值 ÷ 每元保额趸缴净费率}。费率是「每 1 元保额
 * 对应的趸缴净保费」，故<b>不得</b>按贷款策略的「比例」约束为 (0,1]——高龄保单每元保额的
 * 趸缴净保费可以大于 1（同等现金价值换得更少保额）。本聚合只守「必须为正」这一条不变量，
 * 与消费侧 {@code ReducedPaidUpDetail} 的紧凑构造器取值口径保持同源。
 * </p>
 * <p>
 * 🔴 <b>残余开放项（显式登记，非缺陷）</b>：本策略为<b>标量费率</b>，不含保单年度维度。
 * 真实精算口径下每元保额趸缴净费率随年龄/保单年度递增，故同一产品在保单存续期内只能配一条
 * 生效策略。之所以不在此处引入保单年度维度：消费侧契约
 * {@code ReducedPaidUpDetail(cashValue, originalSumInsured, netSinglePremiumRatePerUnit)}
 * 已是标量；且保单年度的权威口径在保单域（{@code PolicyCashValueQueryAppService.policyYearOf}），
 * 在保单域之外复算保单年度必然与现金价值口径漂移。待精算确认分年度费率口径后，
 * 由保单域统一提供保单年度入参再行扩展。
 * </p>
 */
@Getter
public final class ReducedPaidUpPolicy {

    private final String policyId;
    private final String productId;
    private final String policyCode;
    private final String policyVersion;
    private final BigDecimal netSinglePremiumRatePerUnit;
    private final LocalDateTime effectiveFrom;
    private final LocalDateTime effectiveTo;
    private final String tenantId;
    private ActuarialDefinitionStatus status;
    private String contentHash;

    private ReducedPaidUpPolicy(
            String policyId,
            String productId,
            String policyCode,
            String policyVersion,
            BigDecimal netSinglePremiumRatePerUnit,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId,
            ActuarialDefinitionStatus status,
            String contentHash) {
        this.policyId = requireText(policyId, "减额缴清策略ID");
        this.productId = requireText(productId, "产品ID");
        this.policyCode = requireText(policyCode, "减额缴清策略编码").toUpperCase(Locale.ROOT);
        this.policyVersion = requireText(policyVersion, "减额缴清策略版本");
        this.netSinglePremiumRatePerUnit = requirePositiveRate(netSinglePremiumRatePerUnit);
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "生效时间不能为空");
        this.effectiveTo = effectiveTo;
        this.tenantId = requireText(tenantId, "租户ID");
        this.status = Objects.requireNonNull(status, "生命周期状态不能为空");
        this.contentHash = contentHash == null ? "" : contentHash;
        if (effectiveTo != null && !effectiveTo.isAfter(effectiveFrom)) {
            throw invalid("失效时间必须晚于生效时间");
        }
    }

    public static ReducedPaidUpPolicy createDraft(
            String policyId,
            String productId,
            String policyCode,
            String policyVersion,
            BigDecimal netSinglePremiumRatePerUnit,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId) {
        return new ReducedPaidUpPolicy(
                policyId, productId, policyCode, policyVersion, netSinglePremiumRatePerUnit,
                effectiveFrom, effectiveTo, tenantId, ActuarialDefinitionStatus.DRAFT, "");
    }

    public static ReducedPaidUpPolicy restore(
            String policyId,
            String productId,
            String policyCode,
            String policyVersion,
            BigDecimal netSinglePremiumRatePerUnit,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId,
            ActuarialDefinitionStatus status,
            String contentHash) {
        return new ReducedPaidUpPolicy(
                policyId, productId, policyCode, policyVersion, netSinglePremiumRatePerUnit,
                effectiveFrom, effectiveTo, tenantId, status, contentHash);
    }

    public String approve() {
        requireStatus(ActuarialDefinitionStatus.DRAFT, "只有草稿减额缴清策略可以审批");
        contentHash = hash(canonicalContent());
        status = ActuarialDefinitionStatus.APPROVED;
        return contentHash;
    }

    public void publish() {
        requireStatus(ActuarialDefinitionStatus.APPROVED, "只有已审批减额缴清策略可以发布");
        status = ActuarialDefinitionStatus.PUBLISHED;
    }

    public void retire() {
        requireStatus(ActuarialDefinitionStatus.PUBLISHED, "只有已发布减额缴清策略可以退役");
        status = ActuarialDefinitionStatus.RETIRED;
    }

    /** 业务时点是否适用（已发布且落入生效区间）。 */
    public boolean isEffectiveAt(LocalDateTime businessTime) {
        return status == ActuarialDefinitionStatus.PUBLISHED && businessTime != null
                && !businessTime.isBefore(effectiveFrom)
                && (effectiveTo == null || businessTime.isBefore(effectiveTo));
    }

    private String canonicalContent() {
        return String.join("|", productId, policyCode, policyVersion,
                decimal(netSinglePremiumRatePerUnit), effectiveFrom.toString(),
                effectiveTo == null ? "*" : effectiveTo.toString(), tenantId);
    }

    private void requireStatus(ActuarialDefinitionStatus expected, String message) {
        if (status != expected) {
            throw invalid(message);
        }
    }

    /**
     * 每元保额趸缴净费率必须为正。
     * <p>
     * 🔴 <b>刻意不设上界</b>：费率是「每 1 元保额的趸缴净保费」而非比例，高龄/高保单年度下
     * 大于 1 属正常业务事实；误加 {@code <= 1} 约束会把合法配置判为非法，且该误判只在
     * 配置发布环节暴露（放款/减额缴清时已无从配置）。
     * </p>
     */
    private static BigDecimal requirePositiveRate(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            throw invalid("每元保额趸缴净费率必须大于零");
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
