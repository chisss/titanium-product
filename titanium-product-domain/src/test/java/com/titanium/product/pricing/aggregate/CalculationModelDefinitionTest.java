package com.titanium.product.pricing.aggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.common.enums.CalculationNodeType;
import com.titanium.product.common.enums.CalculationOperator;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.pricing.calculation.CalculationEdge;
import com.titanium.product.valueobject.pricing.calculation.CalculationNode;

class CalculationModelDefinitionTest {

    @Test
    void shouldApproveValidDagAndProduceStableReference() {
        CalculationModelDefinition model = model(List.of(
                new CalculationEdge("BASE", "LOAD"),
                new CalculationEdge("BASE", "TOTAL"),
                new CalculationEdge("LOAD", "TOTAL")));

        model.approve();
        model.publish();

        assertEquals(ActuarialDefinitionStatus.PUBLISHED, model.getStatus());
        assertEquals("MODEL-V2A", model.toRef().modelCode());
        assertEquals(List.of("BASE", "LOAD", "TOTAL"),
                model.topologicalNodes().stream().map(CalculationNode::nodeCode).toList());
    }

    @Test
    void shouldRejectCyclicDag() {
        CalculationModelDefinition model = model(List.of(
                new CalculationEdge("BASE", "LOAD"),
                new CalculationEdge("LOAD", "BASE"),
                new CalculationEdge("LOAD", "TOTAL")));

        assertThrows(PricingDomainException.class, model::approve);
    }

    private CalculationModelDefinition model(List<CalculationEdge> edges) {
        return CalculationModelDefinition.createDraft(
                "MODEL-1", "PRODUCT-1", "MODEL-V2A", "V1", "基础费用模型", "测试模型", "CNY",
                List.of(
                        new CalculationNode("BASE", "基础保费", CalculationNodeType.INPUT,
                                CalculationOperator.STANDARD_PREMIUM, "BASE_PREMIUM", "V1", null, 10),
                        new CalculationNode("LOAD", "费用加载", CalculationNodeType.COMPUTE,
                                CalculationOperator.PERCENTAGE_OF, "EXPENSE_LOADING", "V1",
                                new BigDecimal("0.10"), 20),
                        new CalculationNode("TOTAL", "客户应付", CalculationNodeType.OUTPUT,
                                CalculationOperator.SUM, null, null, null, 100)),
                edges, LocalDateTime.of(2026, 1, 1, 0, 0), null, "TENANT-1");
    }
}
