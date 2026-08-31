package com.titanium.product.application.orchestration.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.product.aggregate.CalculationModelDefinition;
import com.titanium.product.aggregate.ChargeComponentDefinition;
import com.titanium.product.aggregate.PricingPlanDefinition;
import com.titanium.product.aggregate.TaxPolicyDefinition;
import com.titanium.product.common.enums.CalculationNodeType;
import com.titanium.product.common.enums.CalculationOperator;
import com.titanium.product.common.enums.ChargeCalculationSource;
import com.titanium.product.common.enums.PricingPlanStatus;
import com.titanium.product.common.enums.TaxPriceMode;
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
import com.titanium.product.valueobject.pricing.CalculationEdge;
import com.titanium.product.valueobject.pricing.CalculationModelRef;
import com.titanium.product.valueobject.pricing.CalculationNode;
import com.titanium.product.valueobject.pricing.PricingRoundingRule;
import com.titanium.product.valueobject.pricing.PricingRuleArtifactRef;
import com.titanium.product.valueobject.pricing.PricingRuleComputationResult;
import com.titanium.product.valueobject.pricing.TaxPolicyRef;

class PricingPlanCalculatorActuarialTest {

    private static final LocalDateTime BUSINESS_TIME = LocalDateTime.of(2026, 2, 1, 0, 0);

    @Test
    void shouldSeparateCustomerTaxAndInternalCommission() {
        CalculationModelDefinition model = publishedModel();
        ChargeComponentDefinition base = publishedComponent(
                "BASE", ChargeCategory.RISK_PREMIUM, AmountChannel.CUSTOMER_PRICE,
                ChargePayerType.POLICYHOLDER, ChargeCalculationSource.BASE_PREMIUM);
        ChargeComponentDefinition tax = publishedComponent(
                "TAX", ChargeCategory.TAX, AmountChannel.CUSTOMER_PRICE,
                ChargePayerType.POLICYHOLDER, ChargeCalculationSource.PERCENTAGE);
        ChargeComponentDefinition commission = publishedComponent(
                "COMMISSION", ChargeCategory.COMMISSION, AmountChannel.INTERNAL_COST,
                ChargePayerType.CHANNEL, ChargeCalculationSource.PERCENTAGE);
        CalculationModelRepository modelRepository = mock(CalculationModelRepository.class);
        ChargeComponentRepository componentRepository = mock(ChargeComponentRepository.class);
        RuleComputationPort rulePort = mock(RuleComputationPort.class);
        when(modelRepository.findPublished("TENANT-1", "PRODUCT-1", "MODEL-V2A", "V1", BUSINESS_TIME))
                .thenReturn(Optional.of(model));
        when(componentRepository.findPublished(eq("TENANT-1"), eq("PRODUCT-1"), eq("BASE"), eq("V1"), any()))
                .thenReturn(Optional.of(base));
        when(componentRepository.findPublished(eq("TENANT-1"), eq("PRODUCT-1"), eq("TAX"), eq("V1"), any()))
                .thenReturn(Optional.of(tax));
        when(componentRepository.findPublished(
                eq("TENANT-1"), eq("PRODUCT-1"), eq("COMMISSION"), eq("V1"), any()))
                .thenReturn(Optional.of(commission));
        when(rulePort.compute(any())).thenReturn(new PricingRuleComputationResult(
                "PLAN-1:BASE", "formula", "V1", "input:1", new BigDecimal("100.00"),
                Map.of(), List.of("formula"), "artifact-hash", 3));
        PricingPlanCalculator calculator = new PricingPlanCalculator(
                mock(RateTableSnapshotRepository.class), new RateTableMatchingService(),
                new PremiumCompositionService(), mock(FeatureResolutionPort.class), rulePort,
                modelRepository, componentRepository,
                new CalculationModelExecutionService(new CalculationTotalsService()),
                mock(TaxPolicyRepository.class), new TaxCalculationService(new CalculationTotalsService()),
                mock(CommissionResolutionPort.class),
                new CommissionCalculationService(new CalculationTotalsService()),
                mock(DynamicFactorRepository.class));

        PricingCalculationOutcome outcome = calculator.calculatePublished(
                publishedPlan(model), new PricingCalculationInput(
                        "TENANT-1", "PRODUCT-1", "REQ-1", BUSINESS_TIME, "CNY",
                        new BigDecimal("100000"), 35, "M", 10, 20, 1, Map.of()), false);

        assertEquals(new BigDecimal("100.00"), outcome.breakdown().totals().premiumSubtotal());
        assertEquals(new BigDecimal("10.00"), outcome.breakdown().totals().taxAndLevyTotal());
        assertEquals(new BigDecimal("110.00"), outcome.totalPremium());
        assertEquals(new BigDecimal("20.00"), outcome.breakdown().totals().internalCostTotal());
    }

    @Test
    void shouldApplyExactlyReferencedExclusiveTaxPolicy() {
        RuleComputationPort rulePort = mock(RuleComputationPort.class);
        TaxPolicyRepository taxPolicyRepository = mock(TaxPolicyRepository.class);
        TaxPolicyDefinition taxPolicy = publishedTaxPolicy();
        when(rulePort.compute(any())).thenReturn(new PricingRuleComputationResult(
                "PLAN-2:BASE", "formula", "V1", "input:1", new BigDecimal("100.00"),
                Map.of(), List.of("formula"), "artifact-hash", 3));
        when(taxPolicyRepository.findPublished(
                "TENANT-1", "PRODUCT-1", "PREMIUM_TAX", "V1", BUSINESS_TIME))
                .thenReturn(Optional.of(taxPolicy));
        PricingPlanCalculator calculator = new PricingPlanCalculator(
                mock(RateTableSnapshotRepository.class), new RateTableMatchingService(),
                new PremiumCompositionService(), mock(FeatureResolutionPort.class), rulePort,
                mock(CalculationModelRepository.class), mock(ChargeComponentRepository.class),
                new CalculationModelExecutionService(new CalculationTotalsService()), taxPolicyRepository,
                new TaxCalculationService(new CalculationTotalsService()), mock(CommissionResolutionPort.class),
                new CommissionCalculationService(new CalculationTotalsService()),
                mock(DynamicFactorRepository.class));
        PricingPlanDefinition plan = PricingPlanDefinition.restore(
                "PLAN-2", "PRODUCT-1", "V1.0", "P2", PricingMode.ACTUARIAL_FORMULA,
                PricingPlanStatus.PUBLISHED, "CNY", LocalDateTime.of(2026, 1, 1, 0, 0), null,
                null, null, new PricingRuleArtifactRef("formula", "V1", "INPUT-V1", "artifact-hash"),
                null, List.of(new TaxPolicyRef("PREMIUM_TAX", "V1", taxPolicy.getContentHash())),
                new PricingRoundingRule(2, RoundingMode.HALF_UP), "TENANT-1", List.of(), "plan-hash");

        PricingCalculationOutcome outcome = calculator.calculatePublished(
                plan, new PricingCalculationInput(
                        "TENANT-1", "PRODUCT-1", "REQ-2", BUSINESS_TIME, "CNY",
                        new BigDecimal("100000"), 35, "M", 10, 20, 1, Map.of()), false);

        assertEquals(new BigDecimal("106.00"), outcome.totalPremium());
        assertEquals("PREMIUM_TAX", outcome.breakdown().lines().getLast().componentCode());
        assertEquals(taxPolicy.getContentHash(), outcome.breakdown().lines().getLast().taxEvidence().policyHash());
    }

    private PricingPlanDefinition publishedPlan(CalculationModelDefinition model) {
        return PricingPlanDefinition.restore(
                "PLAN-1", "PRODUCT-1", "V1.0", "P1", PricingMode.ACTUARIAL_FORMULA,
                PricingPlanStatus.PUBLISHED, "CNY", LocalDateTime.of(2026, 1, 1, 0, 0), null,
                null, null, new PricingRuleArtifactRef("formula", "V1", "INPUT-V1", "artifact-hash"),
                new CalculationModelRef("MODEL-V2A", "V1", model.getContentHash()),
                new PricingRoundingRule(2, RoundingMode.HALF_UP), "TENANT-1", List.of(), "plan-hash");
    }

    private CalculationModelDefinition publishedModel() {
        CalculationModelDefinition model = CalculationModelDefinition.createDraft(
                "MODEL-1", "PRODUCT-1", "MODEL-V2A", "V1", "完整费用模型", "测试", "CNY",
                List.of(
                        node("BASE", CalculationNodeType.INPUT, CalculationOperator.STANDARD_PREMIUM,
                                "BASE", null, 10),
                        node("TAX", CalculationNodeType.COMPUTE, CalculationOperator.PERCENTAGE_OF,
                                "TAX", new BigDecimal("0.10"), 20),
                        node("COMMISSION", CalculationNodeType.COMPUTE, CalculationOperator.PERCENTAGE_OF,
                                "COMMISSION", new BigDecimal("0.20"), 30),
                        new CalculationNode("TOTAL", "客户应付", CalculationNodeType.OUTPUT,
                                CalculationOperator.SUM, null, null, null, 100)),
                List.of(
                        new CalculationEdge("BASE", "TAX"),
                        new CalculationEdge("BASE", "COMMISSION"),
                        new CalculationEdge("BASE", "TOTAL"),
                        new CalculationEdge("TAX", "TOTAL")),
                LocalDateTime.of(2026, 1, 1, 0, 0), null, "TENANT-1");
        model.approve();
        model.publish();
        return model;
    }

    private ChargeComponentDefinition publishedComponent(
            String code,
            ChargeCategory category,
            AmountChannel channel,
            ChargePayerType payer,
            ChargeCalculationSource source) {
        ChargeComponentDefinition component = ChargeComponentDefinition.createDraft(
                "ID-" + code, "PRODUCT-1", code, "V1", code, code, category, channel,
                ChargeDirection.DEBIT, payer, source,
                channel == AmountChannel.CUSTOMER_PRICE ? "PREMIUM" : "COMMISSION_PAYABLE",
                channel == AmountChannel.CUSTOMER_PRICE,
                LocalDateTime.of(2026, 1, 1, 0, 0), null, "TENANT-1");
        component.approve();
        component.publish();
        return component;
    }

    private TaxPolicyDefinition publishedTaxPolicy() {
        TaxPolicyDefinition policy = TaxPolicyDefinition.createDraft(
                "TAX-1", "PRODUCT-1", "PREMIUM_TAX", "V1", "保费税", "",
                "GLOBAL", ChargeCategory.TAX, ChargePayerType.POLICYHOLDER, TaxPriceMode.EXCLUSIVE,
                new BigDecimal("0.06"), List.of("LEGACY_BASE_PREMIUM"), "TAX_PAYABLE", "REG-1", null,
                LocalDateTime.of(2026, 1, 1, 0, 0), null, "TENANT-1");
        policy.approve();
        policy.publish();
        return policy;
    }

    private CalculationNode node(
            String code,
            CalculationNodeType type,
            CalculationOperator operator,
            String component,
            BigDecimal parameter,
            int order) {
        return new CalculationNode(code, code, type, operator, component, "V1", parameter, order);
    }
}
