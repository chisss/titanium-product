package com.titanium.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.product.valueobject.pricing.CalculationLine;
import com.titanium.product.valueobject.pricing.CalculationModelExecutionResult;
import com.titanium.product.valueobject.pricing.CommissionResolutionInstruction;
import com.titanium.product.valueobject.pricing.CommissionResolutionResult;

class CommissionCalculationServiceTest {

    private final CommissionCalculationService service =
            new CommissionCalculationService(new CalculationTotalsService());

    @Test
    void shouldAppendCommissionOnlyToInternalCost() {
        CalculationLine premium = new CalculationLine(
                "BASE", "BASE", "V1", ChargeCategory.RISK_PREMIUM, AmountChannel.CUSTOMER_PRICE,
                ChargeDirection.DEBIT, ChargePayerType.POLICYHOLDER, "PREMIUM", "CNY",
                new BigDecimal("100.00"), null, new BigDecimal("100.00"), "BASE", true, "基础保费");
        CalculationModelExecutionResult base = new CalculationModelExecutionResult(
                List.of(premium), new CalculationTotalsService().summarize(List.of(premium)), "CUSTOMER_PAYABLE");
        CommissionResolutionResult commission = new CommissionResolutionResult(
                "DIRECT_SALES", "V1", "a".repeat(64), "CHANNEL-1", "PRODUCT-1", "CNY",
                new BigDecimal("100.00"), new BigDecimal("10.00"),
                List.of(new CommissionResolutionInstruction(
                        "AGENT", "AGENT-1", BigDecimal.ONE, new BigDecimal("10.00"), 2, 6)));

        CalculationModelExecutionResult result = service.append(base, commission);

        assertEquals(new BigDecimal("100.00"), result.totals().customerPayable());
        assertEquals(new BigDecimal("10.00"), result.totals().internalCostTotal());
        assertFalse(result.lines().getLast().customerVisible());
        assertEquals("a".repeat(64), result.lines().getLast().commissionEvidence().schemeHash());
    }
}
