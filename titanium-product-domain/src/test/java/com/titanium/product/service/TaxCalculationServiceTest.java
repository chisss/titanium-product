package com.titanium.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import com.titanium.product.aggregate.TaxPolicyDefinition;
import com.titanium.product.common.enums.TaxPriceMode;
import com.titanium.product.valueobject.pricing.CalculationLine;
import com.titanium.product.valueobject.pricing.PricingRoundingRule;

class TaxCalculationServiceTest {

    private final TaxCalculationService service = new TaxCalculationService(new CalculationTotalsService());
    private final PricingRoundingRule rounding = new PricingRoundingRule(2, RoundingMode.HALF_UP);

    @Test
    void shouldAddExclusiveTaxToCustomerPayable() {
        var result = service.apply(
                List.of(baseLine(new BigDecimal("100.00"))),
                List.of(policy(TaxPriceMode.EXCLUSIVE, null)), Map.of(), "CNY", rounding);

        assertEquals(new BigDecimal("100.00"), result.totals().premiumSubtotal());
        assertEquals(new BigDecimal("6.00"), result.totals().taxAndLevyTotal());
        assertEquals(new BigDecimal("106.00"), result.totals().customerPayable());
        assertEquals(2, result.lines().size());
    }

    @Test
    void shouldExtractInclusiveTaxWithoutIncreasingCustomerPayable() {
        var result = service.apply(
                List.of(baseLine(new BigDecimal("106.00"))),
                List.of(policy(TaxPriceMode.INCLUSIVE, null)), Map.of(), "CNY", rounding);

        assertEquals(new BigDecimal("100.00"), result.totals().premiumSubtotal());
        assertEquals(new BigDecimal("6.00"), result.totals().taxAndLevyTotal());
        assertEquals(new BigDecimal("106.00"), result.totals().customerPayable());
        assertEquals(false, result.lines().get(1).affectsCustomerPayable());
    }

    @Test
    void shouldPersistExplicitZeroTaxLineWhenExempt() {
        var result = service.apply(
                List.of(baseLine(new BigDecimal("100.00"))),
                List.of(policy(TaxPriceMode.EXCLUSIVE, "taxExempt")),
                Map.of("taxExempt", true), "CNY", rounding);

        assertEquals(BigDecimal.ZERO.setScale(2), result.lines().get(1).calculatedAmount());
        assertEquals(true, result.lines().get(1).taxEvidence().exempt());
        assertEquals(new BigDecimal("100.00"), result.totals().customerPayable());
    }

    private CalculationLine baseLine(BigDecimal amount) {
        return new CalculationLine(
                "BASE", "BASE_PREMIUM", "V1", ChargeCategory.RISK_PREMIUM,
                AmountChannel.CUSTOMER_PRICE, ChargeDirection.DEBIT, ChargePayerType.POLICYHOLDER,
                "PREMIUM", "CNY", null, null, amount, "BASE", true, "基础保费");
    }

    private TaxPolicyDefinition policy(TaxPriceMode mode, String exemptionFeature) {
        TaxPolicyDefinition policy = TaxPolicyDefinition.createDraft(
                "tax-1", "product-1", "PREMIUM_TAX", "V1", "保费税", "",
                "GLOBAL", ChargeCategory.TAX, ChargePayerType.POLICYHOLDER, mode,
                new BigDecimal("0.06"), List.of("BASE_PREMIUM"), "TAX_PAYABLE", "REG-REF-1",
                exemptionFeature, LocalDateTime.of(2026, 1, 1, 0, 0), null, "tenant-1");
        policy.approve();
        policy.publish();
        return policy;
    }
}
