package com.titanium.product.aggregate;

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
import com.titanium.product.common.enums.DynamicFactorMissingPolicy;
import com.titanium.product.common.enums.DynamicFactorSourceType;
import com.titanium.product.common.enums.DynamicFactorTransformType;
import com.titanium.product.common.enums.DynamicFactorValueTimePolicy;
import com.titanium.product.exception.PricingDomainException;

/**
 * Product 版本化动态因子聚合。
 */
public class DynamicFactorDefinition {

    private final String factorId;
    private final String productId;
    private final String factorCode;
    private final String factorVersion;
    private final String factorName;
    private final String description;
    private final String featureCode;
    private final String featureDefinitionVersion;
    private final DynamicFactorSourceType sourceType;
    private final DynamicFactorValueTimePolicy valueTimePolicy;
    private final BigDecimal lowerBound;
    private final BigDecimal upperBound;
    private final DynamicFactorMissingPolicy missingPolicy;
    private final BigDecimal defaultValue;
    private final DynamicFactorTransformType transformType;
    private final BigDecimal multiplier;
    private final BigDecimal offset;
    private final boolean replayable;
    private final LocalDateTime effectiveFrom;
    private final LocalDateTime effectiveTo;
    private final String tenantId;
    private ActuarialDefinitionStatus status;
    private String contentHash;

    private DynamicFactorDefinition(
            String factorId,
            String productId,
            String factorCode,
            String factorVersion,
            String factorName,
            String description,
            String featureCode,
            String featureDefinitionVersion,
            DynamicFactorSourceType sourceType,
            DynamicFactorValueTimePolicy valueTimePolicy,
            BigDecimal lowerBound,
            BigDecimal upperBound,
            DynamicFactorMissingPolicy missingPolicy,
            BigDecimal defaultValue,
            DynamicFactorTransformType transformType,
            BigDecimal multiplier,
            BigDecimal offset,
            boolean replayable,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId,
            ActuarialDefinitionStatus status,
            String contentHash) {
        this.factorId = requireText(factorId, "动态因子ID");
        this.productId = requireText(productId, "产品ID");
        this.factorCode = requireText(factorCode, "动态因子编码").toUpperCase(Locale.ROOT);
        this.factorVersion = requireText(factorVersion, "动态因子版本");
        this.factorName = requireText(factorName, "动态因子名称");
        this.description = description == null ? "" : description.trim();
        this.featureCode = requireText(featureCode, "特征编码");
        this.featureDefinitionVersion = requireText(featureDefinitionVersion, "特征定义版本");
        this.sourceType = Objects.requireNonNull(sourceType, "特征来源不能为空");
        this.valueTimePolicy = Objects.requireNonNull(valueTimePolicy, "取值时点策略不能为空");
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.missingPolicy = Objects.requireNonNull(missingPolicy, "缺失策略不能为空");
        this.defaultValue = defaultValue;
        this.transformType = Objects.requireNonNull(transformType, "变换类型不能为空");
        this.multiplier = Objects.requireNonNull(multiplier, "乘数不能为空");
        this.offset = Objects.requireNonNull(offset, "偏移量不能为空");
        this.replayable = replayable;
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "生效时间不能为空");
        this.effectiveTo = effectiveTo;
        this.tenantId = requireText(tenantId, "租户ID");
        this.status = Objects.requireNonNull(status, "生命周期状态不能为空");
        this.contentHash = contentHash == null ? "" : contentHash;
        validate();
    }

    public static DynamicFactorDefinition createDraft(
            String factorId,
            String productId,
            String factorCode,
            String factorVersion,
            String factorName,
            String description,
            String featureCode,
            String featureDefinitionVersion,
            DynamicFactorSourceType sourceType,
            DynamicFactorValueTimePolicy valueTimePolicy,
            BigDecimal lowerBound,
            BigDecimal upperBound,
            DynamicFactorMissingPolicy missingPolicy,
            BigDecimal defaultValue,
            DynamicFactorTransformType transformType,
            BigDecimal multiplier,
            BigDecimal offset,
            boolean replayable,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId) {
        return new DynamicFactorDefinition(
                factorId, productId, factorCode, factorVersion, factorName, description, featureCode,
                featureDefinitionVersion, sourceType, valueTimePolicy, lowerBound, upperBound, missingPolicy,
                defaultValue, transformType, multiplier, offset, replayable, effectiveFrom, effectiveTo,
                tenantId, ActuarialDefinitionStatus.DRAFT, "");
    }

    public static DynamicFactorDefinition restore(
            String factorId,
            String productId,
            String factorCode,
            String factorVersion,
            String factorName,
            String description,
            String featureCode,
            String featureDefinitionVersion,
            DynamicFactorSourceType sourceType,
            DynamicFactorValueTimePolicy valueTimePolicy,
            BigDecimal lowerBound,
            BigDecimal upperBound,
            DynamicFactorMissingPolicy missingPolicy,
            BigDecimal defaultValue,
            DynamicFactorTransformType transformType,
            BigDecimal multiplier,
            BigDecimal offset,
            boolean replayable,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId,
            ActuarialDefinitionStatus status,
            String contentHash) {
        return new DynamicFactorDefinition(
                factorId, productId, factorCode, factorVersion, factorName, description, featureCode,
                featureDefinitionVersion, sourceType, valueTimePolicy, lowerBound, upperBound, missingPolicy,
                defaultValue, transformType, multiplier, offset, replayable, effectiveFrom, effectiveTo,
                tenantId, status, contentHash);
    }

    public String approve() {
        requireStatus(ActuarialDefinitionStatus.DRAFT, "只有草稿动态因子可以审批");
        if (!replayable) {
            throw invalid("计费动态因子必须支持基于 Feature Center 快照重放");
        }
        contentHash = hash(canonicalContent());
        status = ActuarialDefinitionStatus.APPROVED;
        return contentHash;
    }

    public void publish() {
        requireStatus(ActuarialDefinitionStatus.APPROVED, "只有已审批动态因子可以发布");
        status = ActuarialDefinitionStatus.PUBLISHED;
    }

    public void retire() {
        requireStatus(ActuarialDefinitionStatus.PUBLISHED, "只有已发布动态因子可以退役");
        status = ActuarialDefinitionStatus.RETIRED;
    }

    public boolean isEffectiveAt(LocalDateTime businessTime) {
        return status == ActuarialDefinitionStatus.PUBLISHED && businessTime != null
                && !businessTime.isBefore(effectiveFrom)
                && (effectiveTo == null || businessTime.isBefore(effectiveTo));
    }

    public BigDecimal transform(BigDecimal rawValue) {
        BigDecimal value = rawValue;
        if (value == null) {
            value = switch (missingPolicy) {
                case REJECT -> throw invalid("动态因子缺少必填特征: " + featureCode);
                case USE_DEFAULT -> defaultValue;
                case SKIP -> null;
            };
        }
        if (value == null) {
            return null;
        }
        if (lowerBound != null && value.compareTo(lowerBound) < 0
                || upperBound != null && value.compareTo(upperBound) > 0) {
            throw invalid("动态因子原始值超出允许范围: " + factorCode);
        }
        return transformType == DynamicFactorTransformType.IDENTITY
                ? value
                : value.multiply(multiplier).add(offset);
    }

    private void validate() {
        if (lowerBound != null && upperBound != null && lowerBound.compareTo(upperBound) > 0) {
            throw invalid("动态因子下限不能大于上限");
        }
        if (missingPolicy == DynamicFactorMissingPolicy.USE_DEFAULT && defaultValue == null) {
            throw invalid("USE_DEFAULT 缺失策略必须配置默认值");
        }
        if (defaultValue != null && lowerBound != null && defaultValue.compareTo(lowerBound) < 0
                || defaultValue != null && upperBound != null && defaultValue.compareTo(upperBound) > 0) {
            throw invalid("动态因子默认值必须在上下限范围内");
        }
        if (effectiveTo != null && !effectiveTo.isAfter(effectiveFrom)) {
            throw invalid("失效时间必须晚于生效时间");
        }
    }

    private String canonicalContent() {
        return String.join("|", productId, factorCode, factorVersion, factorName, description, featureCode,
                featureDefinitionVersion, sourceType.name(), valueTimePolicy.name(), decimal(lowerBound),
                decimal(upperBound), missingPolicy.name(), decimal(defaultValue), transformType.name(),
                decimal(multiplier), decimal(offset), Boolean.toString(replayable), effectiveFrom.toString(),
                effectiveTo == null ? "*" : effectiveTo.toString(), tenantId);
    }

    private void requireStatus(ActuarialDefinitionStatus expected, String message) {
        if (status != expected) {
            throw invalid(message);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw invalid(fieldName + "不能为空");
        }
        return value.trim();
    }

    private static String decimal(BigDecimal value) {
        return value == null ? "*" : value.stripTrailingZeros().toPlainString();
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

    public String getFactorId() { return factorId; }
    public String getProductId() { return productId; }
    public String getFactorCode() { return factorCode; }
    public String getFactorVersion() { return factorVersion; }
    public String getFactorName() { return factorName; }
    public String getDescription() { return description; }
    public String getFeatureCode() { return featureCode; }
    public String getFeatureDefinitionVersion() { return featureDefinitionVersion; }
    public DynamicFactorSourceType getSourceType() { return sourceType; }
    public DynamicFactorValueTimePolicy getValueTimePolicy() { return valueTimePolicy; }
    public BigDecimal getLowerBound() { return lowerBound; }
    public BigDecimal getUpperBound() { return upperBound; }
    public DynamicFactorMissingPolicy getMissingPolicy() { return missingPolicy; }
    public BigDecimal getDefaultValue() { return defaultValue; }
    public DynamicFactorTransformType getTransformType() { return transformType; }
    public BigDecimal getMultiplier() { return multiplier; }
    public BigDecimal getOffset() { return offset; }
    public boolean isReplayable() { return replayable; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public LocalDateTime getEffectiveTo() { return effectiveTo; }
    public String getTenantId() { return tenantId; }
    public ActuarialDefinitionStatus getStatus() { return status; }
    public String getContentHash() { return contentHash; }
}
