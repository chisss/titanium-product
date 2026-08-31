package com.titanium.product.application.orchestration.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.aggregate.PricingPlanDefinition;
import com.titanium.product.application.service.pricing.PricingPlanTestRunner;
import com.titanium.product.common.enums.PricingFeatureDataType;
import com.titanium.product.common.enums.RateUnit;
import com.titanium.product.port.CommissionResolutionPort;
import com.titanium.product.port.FeatureResolutionPort;
import com.titanium.product.port.RuleComputationPort;
import com.titanium.product.repository.CalculationModelRepository;
import com.titanium.product.repository.ChargeComponentRepository;
import com.titanium.product.repository.DynamicFactorRepository;
import com.titanium.product.repository.RateTableSnapshotRepository;
import com.titanium.product.repository.TaxPolicyRepository;
import com.titanium.product.service.CalculationModelExecutionService;
import com.titanium.product.service.CalculationTotalsService;
import com.titanium.product.service.CommissionCalculationService;
import com.titanium.product.service.PremiumCompositionService;
import com.titanium.product.service.RateTableMatchingService;
import com.titanium.product.service.TaxCalculationService;
import com.titanium.product.valueobject.RateTableRef;
import com.titanium.product.valueobject.RateTableRow;
import com.titanium.product.valueobject.RateTableSnapshot;
import com.titanium.product.valueobject.pricing.CommissionSchemeRef;
import com.titanium.product.valueobject.pricing.PricingFeatureContract;
import com.titanium.product.valueobject.pricing.PricingFeatureRequirement;
import com.titanium.product.valueobject.pricing.PricingFeatureResolution;
import com.titanium.product.valueobject.pricing.PricingFeatureValue;
import com.titanium.product.valueobject.pricing.PricingRoundingRule;
import com.titanium.product.valueobject.pricing.PricingRuleArtifactRef;
import com.titanium.product.valueobject.pricing.PricingRuleComputationResult;
import com.titanium.product.valueobject.pricing.PricingTestCase;

class PricingPlanTestRunnerTest {

    private final RateTableSnapshotRepository rateTableRepository = mock(RateTableSnapshotRepository.class);
    private final FeatureResolutionPort featureResolutionPort = mock(FeatureResolutionPort.class);
    private final RuleComputationPort ruleComputationPort = mock(RuleComputationPort.class);
    private final CommissionResolutionPort commissionResolutionPort = mock(CommissionResolutionPort.class);
    private PricingPlanTestRunner runner;

    @BeforeEach
    void setUp() {
        PricingPlanCalculator calculator = new PricingPlanCalculator(
                rateTableRepository, new RateTableMatchingService(), new PremiumCompositionService(),
                featureResolutionPort, ruleComputationPort, mock(CalculationModelRepository.class),
                mock(ChargeComponentRepository.class),
                new CalculationModelExecutionService(new CalculationTotalsService()),
                mock(TaxPolicyRepository.class), new TaxCalculationService(new CalculationTotalsService()),
                commissionResolutionPort,
                new CommissionCalculationService(new CalculationTotalsService()),
                mock(DynamicFactorRepository.class));
        runner = new PricingPlanTestRunner(calculator);
    }

    @Test
    void shouldPassRateTableRegressionCase() {
        PricingPlanDefinition plan = rateTablePlan(new BigDecimal("150.00"));
        RateTableRow row = new RateTableRow(
                "ROW-1", 18, 61, "M", 20, 20, new BigDecimal("0.0015"), null, null);
        when(rateTableRepository.findEffectiveSnapshot(any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(new RateTableSnapshot(
                        "TABLE-1", "PRODUCT-1", "LIFE_BASE", "V1", RateUnit.SUM_INSURED_RATIO,
                        "CNY", LocalDateTime.of(2026, 1, 1, 0, 0), null, "table-hash", List.of(row))));

        var result = runner.run(plan);

        assertTrue(result.allPassed());
        assertEquals(new BigDecimal("150.00"), result.caseResults().getFirst().actualPremium());
    }

    @Test
    void shouldUseResolvedFeatureAndFixedRuleArtifact() {
        PricingPlanDefinition plan = formulaPlan("artifact-hash");
        when(featureResolutionPort.resolve(any())).thenReturn(new PricingFeatureResolution(
                "snapshot-1",
                List.of(new PricingFeatureValue(
                        "insured.age", PricingFeatureDataType.INTEGER, "RESOLVED", "REQUEST", "age:1",
                        null, List.of(), null, 35L, null, null, null, null, null, null)),
                Map.of("insured.age", "age:1"), List.of(), "lineage-1"));
        when(ruleComputationPort.compute(any())).thenReturn(new PricingRuleComputationResult(
                "PLAN-1:BASE", "formula", "V1", "input:1", new BigDecimal("88.88"),
                Map.of(), List.of("formula"), "artifact-hash", 5));

        var result = runner.run(plan);

        assertTrue(result.allPassed());
        assertEquals(new BigDecimal("88.88"), result.caseResults().getFirst().actualPremium());
    }

    @Test
    void shouldFailCaseWhenArtifactHashDoesNotMatchPlan() {
        PricingPlanDefinition plan = formulaPlan("expected-hash");
        when(featureResolutionPort.resolve(any())).thenReturn(new PricingFeatureResolution(
                "snapshot-1", List.of(), Map.of(), List.of(), "lineage-1"));
        when(ruleComputationPort.compute(any())).thenReturn(new PricingRuleComputationResult(
                "PLAN-1:BASE", "formula", "V1", "input:1", new BigDecimal("88.88"),
                Map.of(), List.of(), "other-hash", 5));

        var result = runner.run(plan);

        assertFalse(result.allPassed());
        assertEquals(ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED.getCode(),
                result.caseResults().getFirst().failureReason());
    }

    @Test
    void shouldPassChannelAndPolicyYearFromRegressionSnapshot() {
        RateTableRow row = new RateTableRow(
                "ROW-1", 18, 61, "M", 20, 20, new BigDecimal("0.0015"), null, null);
        when(rateTableRepository.findEffectiveSnapshot(any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.of(new RateTableSnapshot(
                        "TABLE-1", "PRODUCT-1", "LIFE_BASE", "V1", RateUnit.SUM_INSURED_RATIO,
                        "CNY", LocalDateTime.of(2026, 1, 1, 0, 0), null, "table-hash", List.of(row))));
        PricingPlanDefinition plan = PricingPlanDefinition.createDraft(
                "PLAN-1", "PRODUCT-1", "V1.0", "P1", PricingMode.RATE_TABLE, "CNY",
                LocalDateTime.of(2026, 1, 1, 0, 0), null,
                RateTableRef.of(null, "LIFE_BASE", "V1"), null, null, null, List.of(),
                List.of(new CommissionSchemeRef("CHANNEL-1", "AGENT_STANDARD", "V1", "a".repeat(64))),
                new PricingRoundingRule(2, RoundingMode.HALF_UP), "TENANT-1");
        plan.replaceTestCases(List.of(new PricingTestCase(
                "CASE-1", "COMMISSION", "佣金上下文", LocalDateTime.of(2026, 6, 1, 0, 0),
                new BigDecimal("100000"), 35, "M", 20, 20, 1,
                Map.of("channelId", "CHANNEL-1", "policyYear", 2), new BigDecimal("150"), BigDecimal.ZERO)));
        plan.approve();

        runner.run(plan);

        verify(commissionResolutionPort).calculate(argThat(request ->
                request.channelId().equals("CHANNEL-1") && request.policyYear() == 2));
    }

    private PricingPlanDefinition rateTablePlan(BigDecimal expectedPremium) {
        PricingPlanDefinition plan = PricingPlanDefinition.createDraft(
                "PLAN-1", "PRODUCT-1", "V1.0", "P1", PricingMode.RATE_TABLE, "CNY",
                LocalDateTime.of(2026, 1, 1, 0, 0), null,
                RateTableRef.of(null, "LIFE_BASE", "V1"), null, null,
                new PricingRoundingRule(2, RoundingMode.HALF_UP), "TENANT-1");
        plan.replaceTestCases(List.of(testCase(expectedPremium)));
        plan.approve();
        return plan;
    }

    private PricingPlanDefinition formulaPlan(String artifactHash) {
        PricingFeatureContract contract = new PricingFeatureContract(
                "pricing-features", "V1", List.of(new PricingFeatureRequirement(
                        "insured.age", PricingFeatureDataType.INTEGER, true, "age:1", "BLOCK", "NORMAL")));
        PricingPlanDefinition plan = PricingPlanDefinition.createDraft(
                "PLAN-1", "PRODUCT-1", "V1.0", "P1", PricingMode.ACTUARIAL_FORMULA, "CNY",
                LocalDateTime.of(2026, 1, 1, 0, 0), null, null, contract,
                new PricingRuleArtifactRef("formula", "V1", "input:1", artifactHash),
                new PricingRoundingRule(2, RoundingMode.HALF_UP), "TENANT-1");
        plan.replaceTestCases(List.of(testCase(new BigDecimal("88.88"))));
        plan.approve();
        return plan;
    }

    private PricingTestCase testCase(BigDecimal expectedPremium) {
        return new PricingTestCase(
                "CASE-1", "BASE", "基础用例", LocalDateTime.of(2026, 6, 1, 0, 0),
                new BigDecimal("100000"), 35, "M", 20, 20, 1,
                Map.of("insured.age", 35), expectedPremium, BigDecimal.ZERO);
    }
}
