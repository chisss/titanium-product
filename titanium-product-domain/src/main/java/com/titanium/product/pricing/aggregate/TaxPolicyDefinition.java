package com.titanium.product.pricing.aggregate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.common.enums.TaxPriceMode;
import com.titanium.product.exception.PricingDomainException;

/**
 * Product 版本化税费策略聚合。
 */
public class TaxPolicyDefinition {

    private final String policyId;
    private final String productId;
    private final String policyCode;
    private final String policyVersion;
    private final String policyName;
    private final String description;
    private final String jurisdictionCode;
    private final ChargeCategory category;
    private final ChargePayerType payerType;
    private final TaxPriceMode priceMode;
    private final BigDecimal taxRate;
    private final List<String> baseComponentCodes;
    private final String accountingClass;
    private final String regulatoryReferenceId;
    private final String exemptionFeatureCode;
    private final LocalDateTime effectiveFrom;
    private final LocalDateTime effectiveTo;
    private final String tenantId;
    private ActuarialDefinitionStatus status;
    private String contentHash;

    private TaxPolicyDefinition(
            String policyId,
            String productId,
            String policyCode,
            String policyVersion,
            String policyName,
            String description,
            String jurisdictionCode,
            ChargeCategory category,
            ChargePayerType payerType,
            TaxPriceMode priceMode,
            BigDecimal taxRate,
            List<String> baseComponentCodes,
            String accountingClass,
            String regulatoryReferenceId,
            String exemptionFeatureCode,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId,
            ActuarialDefinitionStatus status,
            String contentHash) {
        this.policyId = requireText(policyId, "税费策略ID");
        this.productId = requireText(productId, "产品ID");
        this.policyCode = requireText(policyCode, "税费策略编码");
        this.policyVersion = requireText(policyVersion, "税费策略版本");
        this.policyName = requireText(policyName, "税费策略名称");
        this.description = description == null ? "" : description.trim();
        this.jurisdictionCode = requireText(jurisdictionCode, "司法辖区").toUpperCase(Locale.ROOT);
        this.category = Objects.requireNonNull(category, "税费分类不能为空");
        this.payerType = Objects.requireNonNull(payerType, "税费承担方不能为空");
        this.priceMode = Objects.requireNonNull(priceMode, "含税模式不能为空");
        this.taxRate = Objects.requireNonNull(taxRate, "税率不能为空");
        this.baseComponentCodes = normalizeBaseCodes(baseComponentCodes);
        this.accountingClass = requireText(accountingClass, "账务分类");
        this.regulatoryReferenceId = requireText(regulatoryReferenceId, "法规依据");
        this.exemptionFeatureCode = normalizeNullable(exemptionFeatureCode);
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "生效时间不能为空");
        this.effectiveTo = effectiveTo;
        this.tenantId = requireText(tenantId, "租户ID");
        this.status = Objects.requireNonNull(status, "生命周期状态不能为空");
        this.contentHash = contentHash == null ? "" : contentHash;
        validate();
    }

    public static TaxPolicyDefinition createDraft(
            String policyId,
            String productId,
            String policyCode,
            String policyVersion,
            String policyName,
            String description,
            String jurisdictionCode,
            ChargeCategory category,
            ChargePayerType payerType,
            TaxPriceMode priceMode,
            BigDecimal taxRate,
            List<String> baseComponentCodes,
            String accountingClass,
            String regulatoryReferenceId,
            String exemptionFeatureCode,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId) {
        return new TaxPolicyDefinition(
                policyId, productId, policyCode, policyVersion, policyName, description, jurisdictionCode,
                category, payerType, priceMode, taxRate, baseComponentCodes, accountingClass,
                regulatoryReferenceId, exemptionFeatureCode, effectiveFrom, effectiveTo, tenantId,
                ActuarialDefinitionStatus.DRAFT, "");
    }

    public static TaxPolicyDefinition restore(
            String policyId,
            String productId,
            String policyCode,
            String policyVersion,
            String policyName,
            String description,
            String jurisdictionCode,
            ChargeCategory category,
            ChargePayerType payerType,
            TaxPriceMode priceMode,
            BigDecimal taxRate,
            List<String> baseComponentCodes,
            String accountingClass,
            String regulatoryReferenceId,
            String exemptionFeatureCode,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId,
            ActuarialDefinitionStatus status,
            String contentHash) {
        return new TaxPolicyDefinition(
                policyId, productId, policyCode, policyVersion, policyName, description, jurisdictionCode,
                category, payerType, priceMode, taxRate, baseComponentCodes, accountingClass,
                regulatoryReferenceId, exemptionFeatureCode, effectiveFrom, effectiveTo, tenantId,
                status, contentHash);
    }

    public String approve() {
        requireStatus(ActuarialDefinitionStatus.DRAFT, "只有草稿税费策略可以审批");
        contentHash = hash(canonicalContent());
        status = ActuarialDefinitionStatus.APPROVED;
        return contentHash;
    }

    public void publish() {
        requireStatus(ActuarialDefinitionStatus.APPROVED, "只有已审批税费策略可以发布");
        status = ActuarialDefinitionStatus.PUBLISHED;
    }

    public void retire() {
        requireStatus(ActuarialDefinitionStatus.PUBLISHED, "只有已发布税费策略可以退役");
        status = ActuarialDefinitionStatus.RETIRED;
    }

    public boolean isEffectiveAt(LocalDateTime businessTime) {
        return status == ActuarialDefinitionStatus.PUBLISHED && businessTime != null
                && !businessTime.isBefore(effectiveFrom)
                && (effectiveTo == null || businessTime.isBefore(effectiveTo));
    }

    private void validate() {
        if (category != ChargeCategory.TAX && category != ChargeCategory.STAMP_DUTY
                && category != ChargeCategory.REGULATORY_LEVY) {
            throw invalid("税费策略只能使用 TAX、STAMP_DUTY 或 REGULATORY_LEVY 分类");
        }
        if (payerType != ChargePayerType.POLICYHOLDER) {
            throw invalid("V2-B 客户税费策略承担方必须是投保人");
        }
        if (taxRate.signum() < 0 || taxRate.compareTo(BigDecimal.ONE) > 0) {
            throw invalid("税率必须在 0 到 1 之间");
        }
        if (effectiveTo != null && !effectiveTo.isAfter(effectiveFrom)) {
            throw invalid("失效时间必须晚于生效时间");
        }
        long uniqueCodes = baseComponentCodes.stream().map(code -> code.toUpperCase(Locale.ROOT)).distinct().count();
        if (uniqueCodes != baseComponentCodes.size()) {
            throw invalid("税基费用项不能重复");
        }
    }

    private String canonicalContent() {
        return String.join("|", productId, policyCode, policyVersion, policyName, description,
                jurisdictionCode, category.name(), payerType.name(), priceMode.name(), decimal(taxRate),
                baseComponentCodes.stream().sorted(Comparator.naturalOrder()).reduce((a, b) -> a + "," + b)
                        .orElseThrow(),
                accountingClass, regulatoryReferenceId, exemptionFeatureCode == null ? "*" : exemptionFeatureCode,
                effectiveFrom.toString(), effectiveTo == null ? "*" : effectiveTo.toString(), tenantId);
    }

    private void requireStatus(ActuarialDefinitionStatus expected, String message) {
        if (status != expected) {
            throw invalid(message);
        }
    }

    private static List<String> normalizeBaseCodes(List<String> values) {
        if (values == null || values.isEmpty() || values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw invalid("税基至少包含一个费用项编码");
        }
        return values.stream().map(String::trim).toList();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw invalid(fieldName + "不能为空");
        }
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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

    public String getPolicyId() { return policyId; }
    public String getProductId() { return productId; }
    public String getPolicyCode() { return policyCode; }
    public String getPolicyVersion() { return policyVersion; }
    public String getPolicyName() { return policyName; }
    public String getDescription() { return description; }
    public String getJurisdictionCode() { return jurisdictionCode; }
    public ChargeCategory getCategory() { return category; }
    public ChargePayerType getPayerType() { return payerType; }
    public TaxPriceMode getPriceMode() { return priceMode; }
    public BigDecimal getTaxRate() { return taxRate; }
    public List<String> getBaseComponentCodes() { return baseComponentCodes; }
    public String getAccountingClass() { return accountingClass; }
    public String getRegulatoryReferenceId() { return regulatoryReferenceId; }
    public String getExemptionFeatureCode() { return exemptionFeatureCode; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public LocalDateTime getEffectiveTo() { return effectiveTo; }
    public String getTenantId() { return tenantId; }
    public ActuarialDefinitionStatus getStatus() { return status; }
    public String getContentHash() { return contentHash; }
}
