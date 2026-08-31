package com.titanium.product.application.orchestration.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.product.aggregate.DynamicFactorDefinition;
import com.titanium.product.aggregate.PricingPlanDefinition;
import com.titanium.product.common.enums.DynamicFactorMissingPolicy;
import com.titanium.product.common.enums.DynamicFactorSourceType;
import com.titanium.product.common.enums.DynamicFactorTransformType;
import com.titanium.product.common.enums.DynamicFactorValueTimePolicy;
import com.titanium.product.common.enums.PricingFeatureDataType;
import com.titanium.product.common.enums.PricingPlanStatus;
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
import com.titanium.product.valueobject.pricing.DynamicFactorRef;
import com.titanium.product.valueobject.pricing.PricingFeatureContract;
import com.titanium.product.valueobject.pricing.PricingFeatureRequirement;
import com.titanium.product.valueobject.pricing.PricingFeatureResolution;
import com.titanium.product.valueobject.pricing.PricingFeatureResolutionRequest;
import com.titanium.product.valueobject.pricing.PricingFeatureValue;
import com.titanium.product.valueobject.pricing.PricingRoundingRule;
import com.titanium.product.valueobject.pricing.PricingRuleArtifactRef;
import com.titanium.product.valueobject.pricing.PricingRuleComputationRequest;
import com.titanium.product.valueobject.pricing.PricingRuleComputationResult;

class PricingPlanCalculatorDynamicFactorTest {

    private static final LocalDateTime BUSINESS_TIME = LocalDateTime.of(2026, 2, 1, 0, 0);

    @Test
    void shouldTransformSnapshotFeatureAndPreserveVersionEvidence() {
        DynamicFactorDefinition factor = publishedFactor();
        FeatureResolutionPort featurePort = mock(FeatureResolutionPort.class);
        RuleComputationPort rulePort = mock(RuleComputationPort.class);
        DynamicFactorRepository factorRepository = mock(DynamicFactorRepository.class);
        when(featurePort.resolve(any())).thenReturn(new PricingFeatureResolution(
                "snapshot-1",
                List.of(new PricingFeatureValue(
                        "vehicleRiskScore", PricingFeatureDataType.DECIMAL, "RESOLVED", "DERIVED", "FV1",
                        BUSINESS_TIME.minusMinutes(1), List.of(), null, null, new BigDecimal("1.2"), null,
                        null, null, null, null)),
                Map.of("vehicleRiskScore", "FV1"), List.of(), "lineage-1"));
        when(factorRepository.findPublished(
                "TENANT-1", "PRODUCT-1", "RISK_FACTOR", "V1", BUSINESS_TIME))
                .thenReturn(Optional.of(factor));
        when(rulePort.compute(any())).thenReturn(new PricingRuleComputationResult(
                "exec-1", "formula", "V1", "INPUT-V1", new BigDecimal("110.00"),
                Map.of(), List.of("formula"), "artifact-hash", 1));
        PricingPlanCalculator calculator = calculator(featurePort, rulePort, factorRepository);

        PricingCalculationOutcome outcome = calculator.calculatePublished(
                publishedPlan(factor), new PricingCalculationInput(
                        "TENANT-1", "PRODUCT-1", "REQ-1", BUSINESS_TIME, "CNY",
                        new BigDecimal("100000"), 35, "M", 1, 1, 1,
                        Map.of("AGE", 40, "customInput", "preserved")), false);

        ArgumentCaptor<PricingRuleComputationRequest> request =
                ArgumentCaptor.forClass(PricingRuleComputationRequest.class);
        ArgumentCaptor<PricingFeatureResolutionRequest> featureRequest =
                ArgumentCaptor.forClass(PricingFeatureResolutionRequest.class);
        verify(featurePort).resolve(featureRequest.capture());
        verify(rulePort).compute(request.capture());
        assertEquals(40, featureRequest.getValue().requestSnapshot().get("AGE"));
        assertEquals(new BigDecimal("100000"), featureRequest.getValue().requestSnapshot().get("SUM_INSURED"));
        assertEquals("M", featureRequest.getValue().requestSnapshot().get("GENDER"));
        assertEquals("preserved", featureRequest.getValue().requestSnapshot().get("customInput"));
        assertEquals(0, new BigDecimal("1.100").compareTo(
                (BigDecimal) request.getValue().variables().get("RISK_FACTOR")));
        assertEquals("snapshot-1", outcome.featureSnapshotId());
        assertEquals(factor.getContentHash(), outcome.dynamicFactorEvidence().getFirst().contentHash());
    }

    private PricingPlanCalculator calculator(
            FeatureResolutionPort featurePort,
            RuleComputationPort rulePort,
            DynamicFactorRepository factorRepository) {
        CalculationTotalsService totalsService = new CalculationTotalsService();
        return new PricingPlanCalculator(
                mock(RateTableSnapshotRepository.class), new RateTableMatchingService(),
                new PremiumCompositionService(), featurePort, rulePort,
                mock(CalculationModelRepository.class), mock(ChargeComponentRepository.class),
                new CalculationModelExecutionService(totalsService), mock(TaxPolicyRepository.class),
                new TaxCalculationService(totalsService), mock(CommissionResolutionPort.class),
                new CommissionCalculationService(totalsService), factorRepository);
    }

    private DynamicFactorDefinition publishedFactor() {
        DynamicFactorDefinition factor = DynamicFactorDefinition.createDraft(
                "factor-1", "PRODUCT-1", "RISK_FACTOR", "V1", "风险因子", "",
                "vehicleRiskScore", "FV1", DynamicFactorSourceType.DERIVED,
                DynamicFactorValueTimePolicy.BUSINESS_TIME, new BigDecimal("0.5"), new BigDecimal("2.0"),
                DynamicFactorMissingPolicy.REJECT, null, DynamicFactorTransformType.LINEAR,
                new BigDecimal("0.5"), new BigDecimal("0.5"), true,
                LocalDateTime.of(2026, 1, 1, 0, 0), null, "TENANT-1");
        factor.approve();
        factor.publish();
        return factor;
    }

    private PricingPlanDefinition publishedPlan(DynamicFactorDefinition factor) {
        PricingFeatureContract contract = new PricingFeatureContract(
                "pricing", "V1", List.of(new PricingFeatureRequirement(
                        factor.getFeatureCode(), PricingFeatureDataType.DECIMAL, true,
                        factor.getFeatureDefinitionVersion(), "REJECT", "INTERNAL")));
        return PricingPlanDefinition.restore(
                "PLAN-1", "PRODUCT-1", "V1.0", "P1", PricingMode.ACTUARIAL_FORMULA,
                PricingPlanStatus.PUBLISHED, "CNY", LocalDateTime.of(2026, 1, 1, 0, 0), null,
                null, contract, new PricingRuleArtifactRef("formula", "V1", "INPUT-V1", "artifact-hash"),
                null, List.of(), List.of(),
                List.of(new DynamicFactorRef("RISK_FACTOR", "V1", factor.getContentHash())),
                new PricingRoundingRule(2, RoundingMode.HALF_UP), "TENANT-1", List.of(), "plan-hash");
    }
}
