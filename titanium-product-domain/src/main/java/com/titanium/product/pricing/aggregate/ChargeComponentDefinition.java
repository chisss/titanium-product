package com.titanium.product.pricing.aggregate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.common.enums.ChargeCalculationSource;
import com.titanium.product.exception.PricingDomainException;

import lombok.Getter;

/**
 * Product 费用项目录中的版本化费用定义。
 */
@Getter
public final class ChargeComponentDefinition {

    private final String componentId;
    private final String productId;
    private final String componentCode;
    private final String componentVersion;
    private final String componentName;
    private final String description;
    private final ChargeCategory category;
    private final AmountChannel amountChannel;
    private final ChargeDirection direction;
    private final ChargePayerType payerType;
    private final ChargeCalculationSource calculationSource;
    private final String accountingClass;
    private final boolean customerVisible;
    private final LocalDateTime effectiveFrom;
    private final LocalDateTime effectiveTo;
    private final String tenantId;
    private ActuarialDefinitionStatus status;
    private String contentHash;

    private ChargeComponentDefinition(
            String componentId,
            String productId,
            String componentCode,
            String componentVersion,
            String componentName,
            String description,
            ChargeCategory category,
            AmountChannel amountChannel,
            ChargeDirection direction,
            ChargePayerType payerType,
            ChargeCalculationSource calculationSource,
            String accountingClass,
            boolean customerVisible,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId,
            ActuarialDefinitionStatus status,
            String contentHash) {
        this.componentId = requireText(componentId, "费用项ID");
        this.productId = requireText(productId, "产品ID");
        this.componentCode = requireText(componentCode, "费用项编码").toUpperCase(Locale.ROOT);
        this.componentVersion = requireText(componentVersion, "费用项版本");
        this.componentName = requireText(componentName, "费用项名称");
        this.description = description == null ? "" : description.trim();
        this.category = Objects.requireNonNull(category, "费用分类不能为空");
        this.amountChannel = Objects.requireNonNull(amountChannel, "金额通道不能为空");
        this.direction = Objects.requireNonNull(direction, "费用方向不能为空");
        this.payerType = Objects.requireNonNull(payerType, "承担方不能为空");
        this.calculationSource = Objects.requireNonNull(calculationSource, "计算来源不能为空");
        this.accountingClass = requireText(accountingClass, "账务分类");
        this.customerVisible = customerVisible;
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "生效时间不能为空");
        this.effectiveTo = effectiveTo;
        this.tenantId = requireText(tenantId, "租户ID");
        this.status = Objects.requireNonNull(status, "费用项状态不能为空");
        this.contentHash = contentHash == null ? "" : contentHash;
        validatePeriod();
        validateChannel();
    }

    public static ChargeComponentDefinition createDraft(
            String componentId,
            String productId,
            String componentCode,
            String componentVersion,
            String componentName,
            String description,
            ChargeCategory category,
            AmountChannel amountChannel,
            ChargeDirection direction,
            ChargePayerType payerType,
            ChargeCalculationSource calculationSource,
            String accountingClass,
            boolean customerVisible,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId) {
        return new ChargeComponentDefinition(
                componentId, productId, componentCode, componentVersion, componentName, description,
                category, amountChannel, direction, payerType, calculationSource, accountingClass,
                customerVisible, effectiveFrom, effectiveTo, tenantId, ActuarialDefinitionStatus.DRAFT, "");
    }

    public static ChargeComponentDefinition restore(
            String componentId,
            String productId,
            String componentCode,
            String componentVersion,
            String componentName,
            String description,
            ChargeCategory category,
            AmountChannel amountChannel,
            ChargeDirection direction,
            ChargePayerType payerType,
            ChargeCalculationSource calculationSource,
            String accountingClass,
            boolean customerVisible,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            String tenantId,
            ActuarialDefinitionStatus status,
            String contentHash) {
        return new ChargeComponentDefinition(
                componentId, productId, componentCode, componentVersion, componentName, description,
                category, amountChannel, direction, payerType, calculationSource, accountingClass,
                customerVisible, effectiveFrom, effectiveTo, tenantId, status, contentHash);
    }

    public String approve() {
        requireStatus(ActuarialDefinitionStatus.DRAFT, "只有草稿费用项可以审批");
        contentHash = hash(canonicalContent());
        status = ActuarialDefinitionStatus.APPROVED;
        return contentHash;
    }

    public void publish() {
        requireStatus(ActuarialDefinitionStatus.APPROVED, "只有已审批费用项可以发布");
        status = ActuarialDefinitionStatus.PUBLISHED;
    }

    public void retire() {
        requireStatus(ActuarialDefinitionStatus.PUBLISHED, "只有已发布费用项可以退役");
        status = ActuarialDefinitionStatus.RETIRED;
    }

    public boolean isEffectiveAt(LocalDateTime businessTime) {
        return status == ActuarialDefinitionStatus.PUBLISHED && businessTime != null
                && !businessTime.isBefore(effectiveFrom)
                && (effectiveTo == null || businessTime.isBefore(effectiveTo));
    }

    private void validatePeriod() {
        if (effectiveTo != null && !effectiveTo.isAfter(effectiveFrom)) {
            throw invalid("费用项失效时间必须晚于生效时间");
        }
    }

    private void validateChannel() {
        if (amountChannel == AmountChannel.INTERNAL_COST && payerType == ChargePayerType.POLICYHOLDER) {
            throw invalid("内部成本不能由投保人承担");
        }
    }

    private String canonicalContent() {
        return String.join("|", productId, componentCode, componentVersion, componentName, description,
                category.name(), amountChannel.name(), direction.name(), payerType.name(),
                calculationSource.name(), accountingClass, Boolean.toString(customerVisible),
                effectiveFrom.toString(), effectiveTo == null ? "*" : effectiveTo.toString(), tenantId);
    }

    private void requireStatus(ActuarialDefinitionStatus expected, String message) {
        if (status != expected) {
            throw new PricingDomainException(ProductErrorCode.ACTUARIAL_COMPONENT_STATUS_INVALID, message);
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalid(field + "不能为空");
        }
        return value.trim();
    }

    private static PricingDomainException invalid(String detail) {
        return new PricingDomainException(ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED, detail);
    }

    private static String hash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("运行环境不支持SHA-256", exception);
        }
    }
}
