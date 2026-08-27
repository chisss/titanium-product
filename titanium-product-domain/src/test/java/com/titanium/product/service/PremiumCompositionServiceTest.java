package com.titanium.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.titanium.product.common.enums.RateUnit;
import com.titanium.product.valueobject.RateTableRow;

class PremiumCompositionServiceTest {

    private final PremiumCompositionService service = new PremiumCompositionService();

    @Test
    void shouldCalculateSumInsuredRatioAndRoundAtEnd() {
        BigDecimal premium = service.calculate(new BigDecimal("123456.78"), RateUnit.SUM_INSURED_RATIO,
                row("0.01234567", null, null));

        assertEquals(new BigDecimal("1524.16"), premium);
    }

    @Test
    void shouldApplyMinimumAndMaximumPremium() {
        assertEquals(new BigDecimal("500.00"), service.calculate(
                new BigDecimal("100000"), RateUnit.PER_THOUSAND_SUM_INSURED,
                row("2.00000000", new BigDecimal("500"), null)));
        assertEquals(new BigDecimal("900.00"), service.calculate(
                new BigDecimal("100000"), RateUnit.SUM_INSURED_RATIO,
                row("0.02000000", null, new BigDecimal("900"))));
    }

    @Test
    void shouldUseRateAsAmountForFixedRateUnit() {
        assertEquals(new BigDecimal("88.89"), service.calculate(
                new BigDecimal("100000"), RateUnit.FIXED_AMOUNT,
                row("88.88800000", null, null)));
    }

    private RateTableRow row(String rate, BigDecimal minimumPremium, BigDecimal maximumPremium) {
        return new RateTableRow(
                "row-1", null, null, "ALL", null, null,
                new BigDecimal(rate), minimumPremium, maximumPremium);
    }
}
