package com.titanium.product.aggregate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.PricingPlanStatus;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.RateTableRef;
import com.titanium.product.valueobject.pricing.CalculationModelRef;
import com.titanium.product.valueobject.pricing.CommissionSchemeRef;
import com.titanium.product.valueobject.pricing.DynamicFactorRef;
import com.titanium.product.valueobject.pricing.PricingFeatureContract;
import com.titanium.product.valueobject.pricing.PricingPlanValidationResult;
import com.titanium.product.valueobject.pricing.PricingRoundingRule;
import com.titanium.product.valueobject.pricing.PricingRuleArtifactRef;
import com.titanium.product.valueobject.pricing.PricingTestCase;
import com.titanium.product.valueobject.pricing.TaxPolicyRef;

/**
 * Product 版本化定价方案聚合。
 */
public class PricingPlanDefinition {

    private final String planId;
    private final String productId;
    private final String productVersion;
    private final String planVersion;
    private final PricingMode mode;
    private final String currency;
    private final LocalDateTime effectiveFrom;
    private final LocalDateTime effectiveTo;
    private final RateTableRef rateTableRef;
    private final PricingFeatureContract featureContract;
    private final PricingRuleArtifactRef artifactRef;
    private final CalculationModelRef calculationModelRef;
    private final List<TaxPolicyRef> taxPolicyRefs;
    private final List<CommissionSchemeRef> commissionSchemeRefs;
    private final List<DynamicFactorRef> dynamicFactorRefs;
    private final PricingRoundingRule roundingRule;
    private final String tenantId;
    private PricingPlanStatus status;
    private List<PricingTestCase> testCases;
    private String contentHash;

    private PricingPlanDefinition(
            String planId,
            String productId,
            String productVersion,
            String planVersion,
            PricingMode mode,
            PricingPlanStatus status,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            RateTableRef rateTableRef,
            PricingFeatureContract featureContract,
            PricingRuleArtifactRef artifactRef,
            CalculationModelRef calculationModelRef,
            List<TaxPolicyRef> taxPolicyRefs,
            List<CommissionSchemeRef> commissionSchemeRefs,
            List<DynamicFactorRef> dynamicFactorRefs,
            PricingRoundingRule roundingRule,
            String tenantId,
            List<PricingTestCase> testCases,
            String contentHash) {
        this.planId = requireText(planId, "定价方案ID");
        this.productId = requireText(productId, "产品ID");
        this.productVersion = requireText(productVersion, "产品版本");
        this.planVersion = requireText(planVersion, "定价方案版本");
        this.mode = Objects.requireNonNull(mode, "定价模式不能为空");
        this.status = Objects.requireNonNull(status, "定价方案状态不能为空");
        this.currency = normalizeCurrency(currency);
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom, "生效时间不能为空");
        this.effectiveTo = effectiveTo;
        this.rateTableRef = rateTableRef;
        this.featureContract = featureContract;
        this.artifactRef = artifactRef;
        this.calculationModelRef = calculationModelRef;
        this.taxPolicyRefs = List.copyOf(taxPolicyRefs == null ? List.of() : taxPolicyRefs);
        this.commissionSchemeRefs = List.copyOf(
                commissionSchemeRefs == null ? List.of() : commissionSchemeRefs);
        this.dynamicFactorRefs = List.copyOf(dynamicFactorRefs == null ? List.of() : dynamicFactorRefs);
        this.roundingRule = Objects.requireNonNull(roundingRule, "舍入规则不能为空");
        this.tenantId = requireText(tenantId, "租户ID");
        this.testCases = List.copyOf(testCases == null ? List.of() : testCases);
        this.contentHash = contentHash == null ? "" : contentHash;
        validateMetadata();
    }

    public static PricingPlanDefinition createDraft(
            String planId,
            String productId,
            String productVersion,
            String planVersion,
            PricingMode mode,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            RateTableRef rateTableRef,
            PricingFeatureContract featureContract,
            PricingRuleArtifactRef artifactRef,
            PricingRoundingRule roundingRule,
            String tenantId) {
        return createDraft(
                planId, productId, productVersion, planVersion, mode, currency, effectiveFrom, effectiveTo,
                rateTableRef, featureContract, artifactRef, null, roundingRule, tenantId);
    }

    public static PricingPlanDefinition createDraft(
            String planId,
            String productId,
            String productVersion,
            String planVersion,
            PricingMode mode,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            RateTableRef rateTableRef,
            PricingFeatureContract featureContract,
            PricingRuleArtifactRef artifactRef,
            CalculationModelRef calculationModelRef,
            PricingRoundingRule roundingRule,
            String tenantId) {
        return createDraft(
                planId, productId, productVersion, planVersion, mode, currency, effectiveFrom, effectiveTo,
                rateTableRef, featureContract, artifactRef, calculationModelRef, List.of(), roundingRule, tenantId);
    }

    public static PricingPlanDefinition createDraft(
            String planId,
            String productId,
            String productVersion,
            String planVersion,
            PricingMode mode,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            RateTableRef rateTableRef,
            PricingFeatureContract featureContract,
            PricingRuleArtifactRef artifactRef,
            CalculationModelRef calculationModelRef,
            List<TaxPolicyRef> taxPolicyRefs,
            PricingRoundingRule roundingRule,
            String tenantId) {
        return createDraft(planId, productId, productVersion, planVersion, mode, currency, effectiveFrom,
                effectiveTo, rateTableRef, featureContract, artifactRef, calculationModelRef, taxPolicyRefs,
                List.of(), roundingRule, tenantId);
    }

    public static PricingPlanDefinition createDraft(
            String planId,
            String productId,
            String productVersion,
            String planVersion,
            PricingMode mode,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            RateTableRef rateTableRef,
            PricingFeatureContract featureContract,
            PricingRuleArtifactRef artifactRef,
            CalculationModelRef calculationModelRef,
            List<TaxPolicyRef> taxPolicyRefs,
            List<CommissionSchemeRef> commissionSchemeRefs,
            PricingRoundingRule roundingRule,
            String tenantId) {
        return createDraft(
                planId, productId, productVersion, planVersion, mode, currency, effectiveFrom, effectiveTo,
                rateTableRef, featureContract, artifactRef, calculationModelRef, taxPolicyRefs,
                commissionSchemeRefs, List.of(), roundingRule, tenantId);
    }

    public static PricingPlanDefinition createDraft(
            String planId,
            String productId,
            String productVersion,
            String planVersion,
            PricingMode mode,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            RateTableRef rateTableRef,
            PricingFeatureContract featureContract,
            PricingRuleArtifactRef artifactRef,
            CalculationModelRef calculationModelRef,
            List<TaxPolicyRef> taxPolicyRefs,
            List<CommissionSchemeRef> commissionSchemeRefs,
            List<DynamicFactorRef> dynamicFactorRefs,
            PricingRoundingRule roundingRule,
            String tenantId) {
        return new PricingPlanDefinition(
                planId, productId, productVersion, planVersion, mode, PricingPlanStatus.DRAFT, currency,
                effectiveFrom, effectiveTo, rateTableRef, featureContract, artifactRef, calculationModelRef,
                taxPolicyRefs, commissionSchemeRefs, dynamicFactorRefs, roundingRule,
                tenantId, List.of(), "");
    }

    public static PricingPlanDefinition restore(
            String planId,
            String productId,
            String productVersion,
            String planVersion,
            PricingMode mode,
            PricingPlanStatus status,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            RateTableRef rateTableRef,
            PricingFeatureContract featureContract,
            PricingRuleArtifactRef artifactRef,
            PricingRoundingRule roundingRule,
            String tenantId,
            List<PricingTestCase> testCases,
            String contentHash) {
        return restore(
                planId, productId, productVersion, planVersion, mode, status, currency, effectiveFrom,
                effectiveTo, rateTableRef, featureContract, artifactRef, null, roundingRule, tenantId,
                testCases, contentHash);
    }

    public static PricingPlanDefinition restore(
            String planId,
            String productId,
            String productVersion,
            String planVersion,
            PricingMode mode,
            PricingPlanStatus status,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            RateTableRef rateTableRef,
            PricingFeatureContract featureContract,
            PricingRuleArtifactRef artifactRef,
            CalculationModelRef calculationModelRef,
            PricingRoundingRule roundingRule,
            String tenantId,
            List<PricingTestCase> testCases,
            String contentHash) {
        return restore(
                planId, productId, productVersion, planVersion, mode, status, currency, effectiveFrom,
                effectiveTo, rateTableRef, featureContract, artifactRef, calculationModelRef, List.of(),
                roundingRule, tenantId, testCases, contentHash);
    }

    public static PricingPlanDefinition restore(
            String planId,
            String productId,
            String productVersion,
            String planVersion,
            PricingMode mode,
            PricingPlanStatus status,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            RateTableRef rateTableRef,
            PricingFeatureContract featureContract,
            PricingRuleArtifactRef artifactRef,
            CalculationModelRef calculationModelRef,
            List<TaxPolicyRef> taxPolicyRefs,
            PricingRoundingRule roundingRule,
            String tenantId,
            List<PricingTestCase> testCases,
            String contentHash) {
        return restore(planId, productId, productVersion, planVersion, mode, status, currency, effectiveFrom,
                effectiveTo, rateTableRef, featureContract, artifactRef, calculationModelRef, taxPolicyRefs,
                List.of(), roundingRule, tenantId, testCases, contentHash);
    }

    public static PricingPlanDefinition restore(
            String planId,
            String productId,
            String productVersion,
            String planVersion,
            PricingMode mode,
            PricingPlanStatus status,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            RateTableRef rateTableRef,
            PricingFeatureContract featureContract,
            PricingRuleArtifactRef artifactRef,
            CalculationModelRef calculationModelRef,
            List<TaxPolicyRef> taxPolicyRefs,
            List<CommissionSchemeRef> commissionSchemeRefs,
            PricingRoundingRule roundingRule,
            String tenantId,
            List<PricingTestCase> testCases,
            String contentHash) {
        return restore(
                planId, productId, productVersion, planVersion, mode, status, currency, effectiveFrom,
                effectiveTo, rateTableRef, featureContract, artifactRef, calculationModelRef, taxPolicyRefs,
                commissionSchemeRefs, List.of(), roundingRule, tenantId, testCases, contentHash);
    }

    public static PricingPlanDefinition restore(
            String planId,
            String productId,
            String productVersion,
            String planVersion,
            PricingMode mode,
            PricingPlanStatus status,
            String currency,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            RateTableRef rateTableRef,
            PricingFeatureContract featureContract,
            PricingRuleArtifactRef artifactRef,
            CalculationModelRef calculationModelRef,
            List<TaxPolicyRef> taxPolicyRefs,
            List<CommissionSchemeRef> commissionSchemeRefs,
            List<DynamicFactorRef> dynamicFactorRefs,
            PricingRoundingRule roundingRule,
            String tenantId,
            List<PricingTestCase> testCases,
            String contentHash) {
        return new PricingPlanDefinition(
                planId, productId, productVersion, planVersion, mode, status, currency, effectiveFrom,
                effectiveTo, rateTableRef, featureContract, artifactRef, calculationModelRef, taxPolicyRefs,
                commissionSchemeRefs, dynamicFactorRefs, roundingRule,
                tenantId, testCases, contentHash);
    }

    /** 整体替换草稿测试用例。 */
    public void replaceTestCases(List<PricingTestCase> newTestCases) {
        requireStatus(PricingPlanStatus.DRAFT, "只有草稿定价方案可以修改测试用例");
        testCases = List.copyOf(Objects.requireNonNull(newTestCases, "测试用例不能为空"));
        contentHash = "";
        validateTestCases();
    }

    /** 审批配置并锁定内容。 */
    public String approve() {
        requireStatus(PricingPlanStatus.DRAFT, "只有草稿定价方案可以审批");
        validateConfiguration();
        status = PricingPlanStatus.APPROVED;
        contentHash = hash(canonicalContent());
        return contentHash;
    }

    /** 使用真实测试结果发布已审批方案。 */
    public void publish(PricingPlanValidationResult validation) {
        requireStatus(PricingPlanStatus.APPROVED, "只有已审批定价方案可以发布");
        if (validation == null || !validation.allPassed()) {
            throw invalid(ProductErrorCode.PRICING_TEST_CASE_FAILED, "全部测试用例通过后才能发布");
        }
        if (!contentHash.equals(validation.planContentHash())) {
            throw invalid(ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED, "测试结果与当前方案版本不一致");
        }
        status = PricingPlanStatus.PUBLISHED;
    }

    /** 退役已发布方案。 */
    public void retire() {
        requireStatus(PricingPlanStatus.PUBLISHED, "只有已发布定价方案可以退役");
        status = PricingPlanStatus.RETIRED;
    }

    public boolean isEffectiveAt(LocalDateTime businessTime) {
        return status == PricingPlanStatus.PUBLISHED
                && isWithinEffectivePeriod(businessTime);
    }

    /** 判断业务时点是否落在方案有效期内，不附加生命周期状态。 */
    public boolean isWithinEffectivePeriod(LocalDateTime businessTime) {
        return businessTime != null
                && !businessTime.isBefore(effectiveFrom)
                && (effectiveTo == null || businessTime.isBefore(effectiveTo));
    }

    private void validateMetadata() {
        if (effectiveTo != null && !effectiveTo.isAfter(effectiveFrom)) {
            throw invalid(ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED, "失效时间必须晚于生效时间");
        }
        if (currency.length() != 3) {
            throw invalid(ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED, "币种必须是3位代码");
        }
        long uniqueTaxPolicies = taxPolicyRefs.stream()
                .map(ref -> ref.policyCode().toLowerCase(Locale.ROOT) + ':' + ref.policyVersion())
                .distinct()
                .count();
        if (uniqueTaxPolicies != taxPolicyRefs.size()) {
            throw invalid(ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED, "税费策略引用不能重复");
        }
        long uniqueCommissionSchemes = commissionSchemeRefs.stream()
                .map(ref -> ref.channelId().toLowerCase(Locale.ROOT))
                .distinct()
                .count();
        if (uniqueCommissionSchemes != commissionSchemeRefs.size()) {
            throw invalid(ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED, "同一渠道只能引用一个佣金方案版本");
        }
        long uniqueDynamicFactors = dynamicFactorRefs.stream()
                .map(ref -> ref.factorCode().toLowerCase(Locale.ROOT))
                .distinct()
                .count();
        if (uniqueDynamicFactors != dynamicFactorRefs.size()) {
            throw invalid(ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED, "动态因子编码不能重复");
        }
    }

    private void validateConfiguration() {
        if (mode == PricingMode.RATE_TABLE && !hasRateTableReference()) {
            throw invalid(ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED, "费率表模式必须引用精确费率表版本");
        }
        if (mode == PricingMode.ACTUARIAL_FORMULA && artifactRef == null) {
            throw invalid(ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED, "精算公式模式必须引用固定规则工件");
        }
        validateTestCases();
        if (featureContract != null) {
            Set<String> codes = featureContract.requirements().stream()
                    .map(requirement -> requirement.featureCode().toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            if (codes.size() != featureContract.requirements().size()) {
                throw invalid(ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED, "特征契约包含重复特征编码");
            }
        }
    }

    private void validateTestCases() {
        if (testCases.isEmpty()) {
            throw invalid(ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED, "至少需要一个发布回归测试用例");
        }
        Set<String> caseCodes = testCases.stream()
                .map(testCase -> testCase.caseCode().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (caseCodes.size() != testCases.size()) {
            throw invalid(ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED, "测试用例编码不能重复");
        }
    }

    private boolean hasRateTableReference() {
        return rateTableRef != null && hasText(rateTableRef.tableCode()) && hasText(rateTableRef.version());
    }

    private String canonicalContent() {
        String featureContent = featureContract == null ? "*" : String.join("|",
                featureContract.contractId(), featureContract.contractVersion(),
                featureContract.requirements().stream()
                        .map(requirement -> String.join(":", requirement.featureCode(), requirement.dataType().name(),
                                Boolean.toString(requirement.required()), nullable(requirement.definitionVersion()),
                                nullable(requirement.missingPolicy()), nullable(requirement.sensitivity())))
                        .sorted()
                        .collect(Collectors.joining(",")));
        String artifactContent = artifactRef == null ? "*" : String.join("|",
                artifactRef.artifactCode(), artifactRef.artifactVersion(), artifactRef.inputSchemaVersion(),
                artifactRef.artifactHash());
        String rateTableContent = rateTableRef == null ? "*" : String.join("|",
                nullable(rateTableRef.tableCode()), nullable(rateTableRef.version()));
        String calculationModelContent = calculationModelRef == null ? "*" : String.join("|",
                calculationModelRef.modelCode(), calculationModelRef.modelVersion(),
                calculationModelRef.contentHash());
        String taxPolicyContent = taxPolicyRefs.stream()
                .map(ref -> String.join(":", ref.policyCode(), ref.policyVersion(), ref.contentHash()))
                .sorted()
                .collect(Collectors.joining(","));
        String commissionSchemeContent = commissionSchemeRefs.stream()
                .map(ref -> String.join(":", ref.channelId(), ref.schemeCode(), ref.schemeVersion(),
                        ref.contentHash()))
                .sorted()
                .collect(Collectors.joining(","));
        String dynamicFactorContent = dynamicFactorRefs.stream()
                .map(ref -> String.join(":", ref.factorCode(), ref.factorVersion(), ref.contentHash()))
                .sorted()
                .collect(Collectors.joining(","));
        String header = String.join("|", productId, productVersion, planVersion, mode.name(), currency,
                effectiveFrom.toString(), nullable(effectiveTo), rateTableContent, featureContent, artifactContent,
                calculationModelContent, taxPolicyContent, commissionSchemeContent, dynamicFactorContent,
                Integer.toString(roundingRule.scale()),
                roundingRule.roundingMode().name());
        String cases = testCases.stream()
                .map(this::canonicalTestCase)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining("\n"));
        return header + "\n" + cases;
    }

    private String canonicalTestCase(PricingTestCase testCase) {
        return String.join("|", testCase.caseCode(), testCase.businessTime().toString(),
                decimal(testCase.sumInsured()), Integer.toString(testCase.age()), testCase.gender(),
                Integer.toString(testCase.paymentTermYears()), Integer.toString(testCase.coverageTermYears()),
                Integer.toString(testCase.paymentPeriods()), canonicalValue(testCase.requestSnapshot()),
                decimal(testCase.expectedPremium()), decimal(testCase.tolerance()));
    }

    private String canonicalValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof BigDecimal decimalValue) {
            return decimal(decimalValue);
        }
        if (value instanceof Map<?, ?> mapValue) {
            return mapValue.entrySet().stream()
                    .sorted(Comparator.comparing(entry -> String.valueOf(entry.getKey())))
                    .map(entry -> String.valueOf(entry.getKey()) + ':' + canonicalValue(entry.getValue()))
                    .collect(Collectors.joining(",", "{", "}"));
        }
        if (value instanceof Collection<?> collectionValue) {
            return collectionValue.stream()
                    .map(this::canonicalValue)
                    .collect(Collectors.joining(",", "[", "]"));
        }
        return String.valueOf(value);
    }

    private String hash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("运行环境不支持SHA-256", exception);
        }
    }

    private void requireStatus(PricingPlanStatus expected, String message) {
        if (status != expected) {
            throw invalid(ProductErrorCode.PRICING_PLAN_STATUS_INVALID, message);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (!hasText(value)) {
            throw invalid(ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED, fieldName + "不能为空");
        }
        return value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeCurrency(String currency) {
        return requireText(currency, "币种").toUpperCase(Locale.ROOT);
    }

    private static String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String nullable(Object value) {
        return value == null ? "*" : value.toString();
    }

    private static PricingDomainException invalid(ProductErrorCode errorCode, String detail) {
        return new PricingDomainException(errorCode, detail);
    }

    public String planId() {
        return planId;
    }

    public String productId() {
        return productId;
    }

    public String productVersion() {
        return productVersion;
    }

    public String planVersion() {
        return planVersion;
    }

    public PricingMode mode() {
        return mode;
    }

    public PricingPlanStatus status() {
        return status;
    }

    public String currency() {
        return currency;
    }

    public LocalDateTime effectiveFrom() {
        return effectiveFrom;
    }

    public LocalDateTime effectiveTo() {
        return effectiveTo;
    }

    public RateTableRef rateTableRef() {
        return rateTableRef;
    }

    public PricingFeatureContract featureContract() {
        return featureContract;
    }

    public PricingRuleArtifactRef artifactRef() {
        return artifactRef;
    }

    public CalculationModelRef calculationModelRef() {
        return calculationModelRef;
    }

    public List<TaxPolicyRef> taxPolicyRefs() {
        return taxPolicyRefs;
    }

    public List<CommissionSchemeRef> commissionSchemeRefs() {
        return commissionSchemeRefs;
    }

    public List<DynamicFactorRef> dynamicFactorRefs() {
        return dynamicFactorRefs;
    }

    public PricingRoundingRule roundingRule() {
        return roundingRule;
    }

    public String tenantId() {
        return tenantId;
    }

    public List<PricingTestCase> testCases() {
        return testCases;
    }

    public String contentHash() {
        return contentHash;
    }

    // ==================== 持久化映射访问器（MapStruct getXxx 约定） ====================

    public String getPlanId() {
        return planId;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductVersion() {
        return productVersion;
    }

    public String getPlanVersion() {
        return planVersion;
    }

    public PricingMode getMode() {
        return mode;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDateTime getEffectiveTo() {
        return effectiveTo;
    }

    public RateTableRef getRateTableRef() {
        return rateTableRef;
    }

    public PricingFeatureContract getFeatureContract() {
        return featureContract;
    }

    public PricingRuleArtifactRef getArtifactRef() {
        return artifactRef;
    }

    public CalculationModelRef getCalculationModelRef() {
        return calculationModelRef;
    }

    public List<TaxPolicyRef> getTaxPolicyRefs() {
        return taxPolicyRefs;
    }

    public List<CommissionSchemeRef> getCommissionSchemeRefs() {
        return commissionSchemeRefs;
    }

    public List<DynamicFactorRef> getDynamicFactorRefs() {
        return dynamicFactorRefs;
    }

    public PricingRoundingRule getRoundingRule() {
        return roundingRule;
    }

    public String getTenantId() {
        return tenantId;
    }

    public PricingPlanStatus getStatus() {
        return status;
    }

    public List<PricingTestCase> getTestCases() {
        return testCases;
    }

    public String getContentHash() {
        return contentHash;
    }
}
