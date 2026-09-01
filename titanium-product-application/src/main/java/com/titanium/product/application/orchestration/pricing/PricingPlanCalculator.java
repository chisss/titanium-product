package com.titanium.product.application.orchestration.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.PricingPlanStatus;
import com.titanium.product.port.pricing.CommissionResolutionPort;
import com.titanium.product.port.pricing.FeatureResolutionPort;
import com.titanium.product.port.pricing.RuleComputationPort;
import com.titanium.product.pricing.aggregate.CalculationModelDefinition;
import com.titanium.product.pricing.aggregate.ChargeComponentDefinition;
import com.titanium.product.pricing.aggregate.DynamicFactorDefinition;
import com.titanium.product.pricing.aggregate.PricingPlanDefinition;
import com.titanium.product.pricing.aggregate.TaxPolicyDefinition;
import com.titanium.product.repository.CalculationModelRepository;
import com.titanium.product.repository.ChargeComponentRepository;
import com.titanium.product.repository.DynamicFactorRepository;
import com.titanium.product.repository.RateTableSnapshotRepository;
import com.titanium.product.repository.TaxPolicyRepository;
import com.titanium.product.service.CalculationModelExecutionService;
import com.titanium.product.service.CommissionCalculationService;
import com.titanium.product.service.PremiumCompositionService;
import com.titanium.product.service.RateTableMatchingService;
import com.titanium.product.service.TaxCalculationService;
import com.titanium.product.valueobject.pricing.calculation.CalculationModelExecutionResult;
import com.titanium.product.valueobject.pricing.calculation.CalculationNode;
import com.titanium.product.valueobject.pricing.commission.CommissionBaseComponent;
import com.titanium.product.valueobject.pricing.commission.CommissionResolutionRequest;
import com.titanium.product.valueobject.pricing.commission.CommissionResolutionResult;
import com.titanium.product.valueobject.pricing.commission.CommissionSchemeRef;
import com.titanium.product.valueobject.pricing.pricing.DynamicFactorEvidence;
import com.titanium.product.valueobject.pricing.pricing.DynamicFactorRef;
import com.titanium.product.valueobject.pricing.pricing.PricingFeatureResolution;
import com.titanium.product.valueobject.pricing.pricing.PricingFeatureResolutionRequest;
import com.titanium.product.valueobject.pricing.pricing.PricingFeatureValue;
import com.titanium.product.valueobject.pricing.pricing.PricingRuleComputationRequest;
import com.titanium.product.valueobject.pricing.pricing.PricingRuleComputationResult;
import com.titanium.product.valueobject.rate.RateTableCriteria;
import com.titanium.product.valueobject.rate.RateTableRow;
import com.titanium.product.valueobject.rate.RateTableSnapshot;

import lombok.RequiredArgsConstructor;

/**
 * Product PricingPlan 在线计算器。
 */
@Service
@RequiredArgsConstructor
public class PricingPlanCalculator {

    private final RateTableSnapshotRepository rateTableSnapshotRepository;
    private final RateTableMatchingService rateTableMatchingService;
    private final PremiumCompositionService premiumCompositionService;
    private final FeatureResolutionPort featureResolutionPort;
    private final RuleComputationPort ruleComputationPort;
    private final CalculationModelRepository calculationModelRepository;
    private final ChargeComponentRepository chargeComponentRepository;
    private final CalculationModelExecutionService calculationModelExecutionService;
    private final TaxPolicyRepository taxPolicyRepository;
    private final TaxCalculationService taxCalculationService;
    private final CommissionResolutionPort commissionResolutionPort;
    private final CommissionCalculationService commissionCalculationService;
    private final DynamicFactorRepository dynamicFactorRepository;

    /** 执行发布前回归计算，允许已审批或已发布方案。 */
    public PricingCalculationOutcome calculateForValidation(
            PricingPlanDefinition plan, PricingCalculationInput input) {
        return calculate(plan, input, true, true);
    }

    /** 执行在线试算，只允许已发布方案。 */
    public PricingCalculationOutcome calculatePublished(
            PricingPlanDefinition plan, PricingCalculationInput input, boolean explain) {
        return calculate(plan, input, explain, false);
    }

    private PricingCalculationOutcome calculate(
            PricingPlanDefinition plan, PricingCalculationInput input, boolean explain, boolean validation) {
        validateInput(plan, input, validation);
        Map<String, Object> variables = standardVariables(input);
        FeatureCalculation featureCalculation = resolveFeatures(plan, input, variables);
        RateCalculation rateCalculation = calculateRateTablePremium(plan, input, variables);
        RuleCalculation ruleCalculation = calculateRule(
                plan, input, variables, rateCalculation.basePremium(), explain);
        BigDecimal standardPremium = ruleCalculation.totalPremium()
                .setScale(plan.roundingRule().scale(), plan.roundingRule().roundingMode());
        CalculationModelExecutionResult breakdown = calculateBreakdown(plan, input, standardPremium, variables);
        breakdown = calculateCommission(plan, input, breakdown);
        BigDecimal totalPremium = breakdown.totals().customerPayable();
        return new PricingCalculationOutcome(
                totalPremium, rateCalculation.matchedRate(), rateCalculation.matchedRowId(),
                rateCalculation.tableCode(), rateCalculation.tableVersion(), rateCalculation.tableContentHash(),
                featureCalculation.snapshotId(), ruleCalculation.artifactCode(), ruleCalculation.artifactVersion(),
                ruleCalculation.artifactHash(), breakdown,
                plan.calculationModelRef() == null ? null : plan.calculationModelRef().modelCode(),
                plan.calculationModelRef() == null ? null : plan.calculationModelRef().modelVersion(),
                plan.calculationModelRef() == null ? null : plan.calculationModelRef().contentHash(),
                featureCalculation.factorEvidence());
    }

    private CalculationModelExecutionResult calculateBreakdown(
            PricingPlanDefinition plan,
            PricingCalculationInput input,
            BigDecimal standardPremium,
            Map<String, Object> variables) {
        CalculationModelExecutionResult baseBreakdown;
        if (plan.calculationModelRef() == null) {
            baseBreakdown = calculationModelExecutionService.legacy(
                    standardPremium, plan.currency(), plan.roundingRule());
        } else {
            CalculationModelDefinition model = calculationModelRepository.findPublished(
                        plan.tenantId(), plan.productId(), plan.calculationModelRef().modelCode(),
                        plan.calculationModelRef().modelVersion(), input.businessTime())
                    .orElseThrow(() -> new BusinessException(ProductErrorCode.ACTUARIAL_MODEL_NOT_FOUND));
            if (!plan.calculationModelRef().contentHash().equals(model.getContentHash())
                    || !plan.currency().equalsIgnoreCase(model.getCurrency())) {
                throw invalidPlan("计算模型版本证据或币种与定价包不一致");
            }
            List<ChargeComponentDefinition> components = resolveComponents(plan, input, model);
            baseBreakdown = calculationModelExecutionService.execute(
                    model, components, standardPremium, plan.roundingRule(), input.businessTime());
        }
        List<TaxPolicyDefinition> taxPolicies = resolveTaxPolicies(plan, input);
        return taxCalculationService.apply(
                baseBreakdown.lines(), taxPolicies, variables, plan.currency(), plan.roundingRule());
    }

    private List<TaxPolicyDefinition> resolveTaxPolicies(
            PricingPlanDefinition plan, PricingCalculationInput input) {
        return plan.taxPolicyRefs().stream().map(reference -> {
            TaxPolicyDefinition policy = taxPolicyRepository.findPublished(
                            plan.tenantId(), plan.productId(), reference.policyCode(), reference.policyVersion(),
                            input.businessTime())
                    .orElseThrow(() -> new BusinessException(ProductErrorCode.ACTUARIAL_COMPONENT_NOT_FOUND));
            if (!reference.contentHash().equals(policy.getContentHash())) {
                throw invalidPlan("税费策略hash与定价包引用不一致: " + reference.policyCode());
            }
            return policy;
        }).toList();
    }

    private CalculationModelExecutionResult calculateCommission(
            PricingPlanDefinition plan,
            PricingCalculationInput input,
            CalculationModelExecutionResult breakdown) {
        if (input.channelId() == null || input.channelId().isBlank() || plan.commissionSchemeRefs().isEmpty()) {
            return breakdown;
        }
        CommissionSchemeRef reference = plan.commissionSchemeRefs().stream()
                .filter(ref -> ref.channelId().equalsIgnoreCase(input.channelId()))
                .findFirst()
                .orElseThrow(() -> invalidPlan("定价包未引用当前渠道的佣金方案"));
        List<CommissionBaseComponent> baseComponents = breakdown.lines().stream()
                .filter(line -> line.calculatedAmount().signum() >= 0)
                .map(line -> new CommissionBaseComponent(line.componentCode(), line.calculatedAmount()))
                .toList();
        CommissionResolutionResult result = commissionResolutionPort.calculate(new CommissionResolutionRequest(
                plan.tenantId(), plan.productId(), input.channelId(), plan.currency(), input.businessTime(),
                input.policyYear(), input.paymentPeriods(), plan.roundingRule().scale(),
                plan.roundingRule().roundingMode(), reference, baseComponents));
        return commissionCalculationService.append(breakdown, result);
    }

    private List<ChargeComponentDefinition> resolveComponents(
            PricingPlanDefinition plan, PricingCalculationInput input, CalculationModelDefinition model) {
        Map<String, ChargeComponentDefinition> components = new LinkedHashMap<>();
        for (CalculationNode node : model.getNodes()) {
            if (!node.hasComponent()) {
                continue;
            }
            String key = node.componentCode() + ':' + node.componentVersion();
            components.computeIfAbsent(key, ignored -> chargeComponentRepository.findPublished(
                            plan.tenantId(), plan.productId(), node.componentCode(), node.componentVersion(),
                            input.businessTime())
                    .orElseThrow(() -> new BusinessException(ProductErrorCode.ACTUARIAL_COMPONENT_NOT_FOUND)));
        }
        return List.copyOf(components.values());
    }

    private void validateInput(PricingPlanDefinition plan, PricingCalculationInput input, boolean validation) {
        if (!plan.tenantId().equals(input.tenantId()) || !plan.productId().equals(input.productId())) {
            throw invalidPlan("定价方案与计算租户或产品不一致");
        }
        boolean statusAllowed = plan.status() == PricingPlanStatus.PUBLISHED
                || validation && plan.status() == PricingPlanStatus.APPROVED;
        if (!statusAllowed) {
            throw invalidPlan(validation ? "发布回归只允许已审批或已发布定价方案" : "定价方案未发布");
        }
        if (!plan.isWithinEffectivePeriod(input.businessTime())) {
            throw invalidPlan("定价方案在业务时点未生效");
        }
        if (!plan.currency().equalsIgnoreCase(input.currency())) {
            throw new BusinessException(ProductErrorCode.PRICING_CURRENCY_MISMATCH);
        }
    }

    private FeatureCalculation resolveFeatures(
            PricingPlanDefinition plan, PricingCalculationInput input, Map<String, Object> variables) {
        if (plan.featureContract() == null || plan.featureContract().requirements().isEmpty()) {
            if (!plan.dynamicFactorRefs().isEmpty()) {
                throw invalidPlan("动态因子必须绑定 Feature Center 特征契约");
            }
            return FeatureCalculation.empty();
        }
        PricingFeatureResolution resolution = featureResolutionPort.resolve(new PricingFeatureResolutionRequest(
                plan.tenantId(), input.executionId(), plan.featureContract().contractId(),
                plan.featureContract().contractVersion(), input.businessTime(),
                plan.featureContract().requirements(), featureRequestSnapshot(input), Map.of()));
        if (!resolution.missingRequired().isEmpty()) {
            throw new BusinessException(
                    "缺少必填特征: " + String.join(",", resolution.missingRequired()),
                    ProductErrorCode.PRICING_INPUT_INVALID);
        }
        resolution.values().stream()
                .filter(value -> "RESOLVED".equalsIgnoreCase(value.status()))
                .forEach(value -> variables.put(value.featureCode(), typedValue(value)));
        List<DynamicFactorEvidence> evidence = applyDynamicFactors(plan, input, resolution, variables);
        return new FeatureCalculation(resolution.snapshotId(), evidence);
    }

    private Map<String, Object> featureRequestSnapshot(PricingCalculationInput input) {
        Map<String, Object> snapshot = new LinkedHashMap<>(input.requestSnapshot());
        snapshot.putIfAbsent("SUM_INSURED", input.sumInsured());
        snapshot.putIfAbsent("AGE", input.age());
        snapshot.putIfAbsent("GENDER", input.gender());
        snapshot.putIfAbsent("PAYMENT_TERM", input.paymentTermYears());
        snapshot.putIfAbsent("COVERAGE_TERM", input.coverageTermYears());
        snapshot.putIfAbsent("PAYMENT_PERIODS", input.paymentPeriods());
        snapshot.putIfAbsent("POLICY_YEAR", input.policyYear());
        return snapshot;
    }

    private List<DynamicFactorEvidence> applyDynamicFactors(
            PricingPlanDefinition plan,
            PricingCalculationInput input,
            PricingFeatureResolution resolution,
            Map<String, Object> variables) {
        Map<String, PricingFeatureValue> values = resolution.values().stream()
                .collect(Collectors.toMap(
                        value -> value.featureCode().toLowerCase(Locale.ROOT),
                        value -> value,
                        (left, right) -> left));
        return plan.dynamicFactorRefs().stream().map(reference -> {
            DynamicFactorDefinition factor = resolveDynamicFactor(plan, input, reference);
            PricingFeatureValue featureValue = values.get(factor.getFeatureCode().toLowerCase(Locale.ROOT));
            validateFeatureVersion(factor, featureValue, resolution, input.businessTime());
            BigDecimal transformed = factor.transform(numericValue(featureValue));
            if (transformed != null) {
                variables.put(factor.getFactorCode(), transformed);
            }
            return new DynamicFactorEvidence(
                    factor.getFactorCode(), factor.getFactorVersion(), factor.getContentHash(),
                    factor.getFeatureCode(), factor.getFeatureDefinitionVersion());
        }).toList();
    }

    private DynamicFactorDefinition resolveDynamicFactor(
            PricingPlanDefinition plan, PricingCalculationInput input, DynamicFactorRef reference) {
        DynamicFactorDefinition factor = dynamicFactorRepository.findPublished(
                        plan.tenantId(), plan.productId(), reference.factorCode(), reference.factorVersion(),
                        input.businessTime())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.ACTUARIAL_COMPONENT_NOT_FOUND));
        if (!reference.contentHash().equals(factor.getContentHash())) {
            throw invalidPlan("动态因子hash与定价包引用不一致: " + reference.factorCode());
        }
        return factor;
    }

    private void validateFeatureVersion(
            DynamicFactorDefinition factor,
            PricingFeatureValue value,
            PricingFeatureResolution resolution,
            LocalDateTime businessTime) {
        String resolvedVersion = value == null ? resolution.definitionVersions().get(factor.getFeatureCode())
                : value.definitionVersion();
        if (!factor.getFeatureDefinitionVersion().equals(resolvedVersion)) {
            throw invalidPlan("Feature Center 返回的特征定义版本不一致: " + factor.getFeatureCode());
        }
        if (value != null && value.observedAt() != null && value.observedAt().isAfter(businessTime)) {
            throw invalidPlan("动态因子特征观测时间晚于业务时点: " + factor.getFeatureCode());
        }
    }

    private BigDecimal numericValue(PricingFeatureValue value) {
        if (value == null || !"RESOLVED".equalsIgnoreCase(value.status())) {
            return null;
        }
        return switch (value.dataType()) {
            case INTEGER -> BigDecimal.valueOf(value.integerValue());
            case DECIMAL -> value.decimalValue();
            case STRING -> parseDecimal(value.featureCode(), value.stringValue());
            default -> throw invalidPlan("动态因子只支持数值特征: " + value.featureCode());
        };
    }

    private BigDecimal parseDecimal(String featureCode, String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw invalidPlan("动态因子特征不是合法数值: " + featureCode);
        }
    }

    private RateCalculation calculateRateTablePremium(
            PricingPlanDefinition plan, PricingCalculationInput input, Map<String, Object> variables) {
        if (plan.rateTableRef() == null) {
            return RateCalculation.empty();
        }
        RateTableCriteria criteria = new RateTableCriteria(
                input.age(), input.gender(), input.paymentTermYears(), input.coverageTermYears());
        RateTableSnapshot snapshot = rateTableSnapshotRepository.findEffectiveSnapshot(
                plan.tenantId(), plan.productId(), plan.rateTableRef().tableCode(),
                        plan.rateTableRef().version(), input.businessTime(), criteria)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.RATE_TABLE_NOT_EFFECTIVE));
        if (!plan.currency().equalsIgnoreCase(snapshot.currency())) {
            throw new BusinessException(ProductErrorCode.PRICING_CURRENCY_MISMATCH);
        }
        RateTableRow row = rateTableMatchingService.match(snapshot, criteria);
        BigDecimal basePremium = premiumCompositionService.calculate(input.sumInsured(), snapshot.rateUnit(), row);
        variables.put("matchedRate", row.rate());
        variables.put("matchedRateRowId", row.rowId());
        variables.put("rateTableContentHash", snapshot.contentHash());
        return new RateCalculation(
                basePremium, row.rate(), row.rowId(), snapshot.tableCode(), snapshot.tableVersion(),
                snapshot.contentHash());
    }

    private RuleCalculation calculateRule(
            PricingPlanDefinition plan,
            PricingCalculationInput input,
            Map<String, Object> variables,
            BigDecimal basePremium,
            boolean explain) {
        if (plan.artifactRef() == null) {
            return new RuleCalculation(basePremium, null, null, null);
        }
        variables.put("basePremium", basePremium);
        PricingRuleComputationResult computation = ruleComputationPort.compute(new PricingRuleComputationRequest(
                plan.tenantId(), input.executionId(), plan.artifactRef().artifactCode(),
                plan.artifactRef().artifactVersion(), plan.artifactRef().inputSchemaVersion(), variables,
                explain, input.businessTime()));
        if (!plan.artifactRef().artifactHash().equals(computation.artifactHash())) {
            throw invalidPlan("规则工件hash与定价方案引用不一致");
        }
        return new RuleCalculation(
                computation.computedValue(), computation.artifactCode(), computation.artifactVersion(),
                computation.artifactHash());
    }

    private Map<String, Object> standardVariables(PricingCalculationInput input) {
        Map<String, Object> variables = new LinkedHashMap<>(input.requestSnapshot());
        variables.put("sumInsured", input.sumInsured());
        variables.put("age", input.age());
        variables.put("gender", input.gender());
        variables.put("paymentTermYears", input.paymentTermYears());
        variables.put("coverageTermYears", input.coverageTermYears());
        variables.put("paymentPeriods", input.paymentPeriods());
        return variables;
    }

    private Object typedValue(PricingFeatureValue value) {
        return switch (value.dataType()) {
            case STRING -> value.stringValue();
            case INTEGER -> value.integerValue();
            case DECIMAL -> value.decimalValue();
            case BOOLEAN -> value.booleanValue();
            case DATE -> value.dateValue();
            case DATETIME -> value.dateTimeValue();
            case ENUM -> value.enumValue();
            case JSON -> value.jsonValue();
        };
    }

    private BusinessException invalidPlan(String detail) {
        return new BusinessException(detail, ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED);
    }

    private record RateCalculation(
            BigDecimal basePremium,
            BigDecimal matchedRate,
            String matchedRowId,
            String tableCode,
            String tableVersion,
            String tableContentHash) {

        private static RateCalculation empty() {
            return new RateCalculation(BigDecimal.ZERO, null, null, null, null, null);
        }
    }

    private record RuleCalculation(
            BigDecimal totalPremium,
            String artifactCode,
            String artifactVersion,
            String artifactHash) {
    }

    private record FeatureCalculation(String snapshotId, List<DynamicFactorEvidence> factorEvidence) {

        private static FeatureCalculation empty() {
            return new FeatureCalculation(null, List.of());
        }
    }
}
