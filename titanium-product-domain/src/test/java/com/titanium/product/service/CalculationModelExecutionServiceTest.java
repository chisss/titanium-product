package com.titanium.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.product.common.enums.CalculationNodeType;
import com.titanium.product.common.enums.CalculationOperator;
import com.titanium.product.common.enums.ChargeCalculationSource;
import com.titanium.product.common.enums.PremiumBalanceDirection;
import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.pricing.aggregate.CalculationModelDefinition;
import com.titanium.product.pricing.aggregate.ChargeComponentDefinition;
import com.titanium.product.pricing.aggregate.PremiumCalculation;
import com.titanium.product.valueobject.pricing.calculation.CalculationEdge;
import com.titanium.product.valueobject.pricing.calculation.CalculationModelExecutionResult;
import com.titanium.product.valueobject.pricing.calculation.CalculationNode;
import com.titanium.product.valueobject.pricing.premium.PremiumCalculationEvidence;
import com.titanium.product.valueobject.pricing.premium.PremiumLifecycleDifference;
import com.titanium.product.valueobject.pricing.pricing.PricingRoundingRule;

class CalculationModelExecutionServiceTest {

    private static final LocalDateTime BUSINESS_TIME = LocalDateTime.of(2026, 2, 1, 0, 0);
    private final CalculationModelExecutionService service =
            new CalculationModelExecutionService(new CalculationTotalsService());

    @Test
    void shouldKeepCommissionOutsideCustomerPayable() {
        List<ChargeComponentDefinition> components = components();
        CalculationModelDefinition model = publishedModel();

        var result = service.execute(
                model, components, new BigDecimal("100.00"),
                new PricingRoundingRule(2, RoundingMode.HALF_UP), BUSINESS_TIME);

        assertEquals(new BigDecimal("115.00"), result.totals().premiumSubtotal());
        assertEquals(new BigDecimal("115.00"), result.totals().customerPayable());
        assertEquals(new BigDecimal("20.00"), result.totals().internalCostTotal());
        assertEquals(4, result.lines().size());
        assertFalse(result.lines().stream()
                .filter(line -> line.amountChannel() == AmountChannel.INTERNAL_COST)
                .findFirst().orElseThrow().affectsCustomerPayable());
    }

    @Test
    void shouldReconcileLifecycleDifferenceFromExecutedBreakdowns() {
        List<ChargeComponentDefinition> components = components();
        CalculationModelDefinition model = publishedModel();
        PricingRoundingRule roundingRule = new PricingRoundingRule(2, RoundingMode.HALF_UP);
        CalculationModelExecutionResult originalBreakdown =
                service.execute(model, components, new BigDecimal("100.00"), roundingRule, BUSINESS_TIME);
        CalculationModelExecutionResult replacementBreakdown =
                service.execute(model, components, new BigDecimal("120.00"), roundingRule, BUSINESS_TIME);

        PremiumLifecycleDifference difference = new PremiumLifecycleDifferenceService().compare(
                calculation("original", PricingCalculationPurpose.ISSUANCE_CONFIRM, originalBreakdown),
                calculation("replacement", PricingCalculationPurpose.MAINTENANCE, replacementBreakdown));

        assertEquals(PremiumBalanceDirection.DEBIT, difference.direction());
        assertEquals(new BigDecimal("22.00"), difference.customerAmount());
        assertEquals(new BigDecimal("4.00"), difference.internalCostAmount());
        assertFalse(difference.lines().stream()
                .filter(line -> line.amountChannel() == AmountChannel.INTERNAL_COST)
                .findFirst().orElseThrow().affectsCustomerPayable());
    }

    private List<ChargeComponentDefinition> components() {
        return List.of(
                publishedComponent("BASE", ChargeCategory.RISK_PREMIUM, AmountChannel.CUSTOMER_PRICE,
                        ChargePayerType.POLICYHOLDER, ChargeCalculationSource.BASE_PREMIUM),
                publishedComponent("EXPENSE", ChargeCategory.EXPENSE_LOADING, AmountChannel.CUSTOMER_PRICE,
                        ChargePayerType.POLICYHOLDER, ChargeCalculationSource.PERCENTAGE),
                publishedComponent("POLICY_FEE", ChargeCategory.PRODUCT_FEE, AmountChannel.CUSTOMER_PRICE,
                        ChargePayerType.POLICYHOLDER, ChargeCalculationSource.FIXED_AMOUNT),
                publishedComponent("COMMISSION", ChargeCategory.COMMISSION, AmountChannel.INTERNAL_COST,
                        ChargePayerType.CHANNEL, ChargeCalculationSource.PERCENTAGE));
    }

    private PremiumCalculation calculation(
            String id,
            PricingCalculationPurpose purpose,
            CalculationModelExecutionResult breakdown) {
        BigDecimal customerPayable = breakdown.totals().customerPayable();
        return PremiumCalculation.confirm(
                id, "request-" + id, "policy-1", purpose, "TENANT-1", "PRODUCT-1",
                BUSINESS_TIME, "CNY", customerPayable, customerPayable, customerPayable, 1,
                List.of(), breakdown.totals(), breakdown.lines(), evidence(), Map.of(),
                hash(id.charAt(0)), hash('i'), hash('r'), BUSINESS_TIME.plusMinutes(1));
    }

    private PremiumCalculationEvidence evidence() {
        return new PremiumCalculationEvidence(
                "V1", "P1", hash('p'), "TABLE", "V1", hash('t'), "feature-1",
                "RULE", "V1", hash('a'), 2, "HALF_UP");
    }

    private String hash(char value) {
        return String.valueOf(value).repeat(64);
    }

    private ChargeComponentDefinition publishedComponent(
            String code,
            ChargeCategory category,
            AmountChannel channel,
            ChargePayerType payer,
            ChargeCalculationSource source) {
        ChargeComponentDefinition component = ChargeComponentDefinition.createDraft(
                "ID-" + code, "PRODUCT-1", code, "V1", code, code,
                category, channel, ChargeDirection.DEBIT, payer, source,
                channel == AmountChannel.CUSTOMER_PRICE ? "PREMIUM" : "COMMISSION_PAYABLE",
                channel == AmountChannel.CUSTOMER_PRICE,
                LocalDateTime.of(2026, 1, 1, 0, 0), null, "TENANT-1");
        component.approve();
        component.publish();
        return component;
    }

    private CalculationModelDefinition publishedModel() {
        CalculationModelDefinition model = CalculationModelDefinition.createDraft(
                "MODEL-1", "PRODUCT-1", "MODEL-V2A", "V1", "完整费用模型", "测试", "CNY",
                List.of(
                        node("BASE", CalculationNodeType.INPUT, CalculationOperator.STANDARD_PREMIUM, "BASE", null, 10),
                        node("EXPENSE", CalculationNodeType.COMPUTE, CalculationOperator.PERCENTAGE_OF,
                                "EXPENSE", new BigDecimal("0.10"), 20),
                        node("POLICY_FEE", CalculationNodeType.COMPUTE, CalculationOperator.FIXED_AMOUNT,
                                "POLICY_FEE", new BigDecimal("5.00"), 30),
                        node("COMMISSION", CalculationNodeType.COMPUTE, CalculationOperator.PERCENTAGE_OF,
                                "COMMISSION", new BigDecimal("0.20"), 40),
                        new CalculationNode("TOTAL", "客户应付", CalculationNodeType.OUTPUT,
                                CalculationOperator.SUM, null, null, null, 100)),
                List.of(
                        new CalculationEdge("BASE", "EXPENSE"),
                        new CalculationEdge("BASE", "COMMISSION"),
                        new CalculationEdge("BASE", "TOTAL"),
                        new CalculationEdge("EXPENSE", "TOTAL"),
                        new CalculationEdge("POLICY_FEE", "TOTAL")),
                LocalDateTime.of(2026, 1, 1, 0, 0), null, "TENANT-1");
        model.approve();
        model.publish();
        return model;
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
