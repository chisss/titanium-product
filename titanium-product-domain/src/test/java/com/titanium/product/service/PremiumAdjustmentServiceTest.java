package com.titanium.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.PremiumAdjustmentType;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.pricing.PremiumAdjustmentRequest;
import com.titanium.product.valueobject.pricing.PricingRoundingRule;

class PremiumAdjustmentServiceTest {

    private final PremiumAdjustmentService service = new PremiumAdjustmentService();
    private final PricingRoundingRule roundingRule = new PricingRoundingRule(2, RoundingMode.HALF_UP);

    @Test
    void shouldApplyAdjustmentsInRequestOrder() {
        var result = service.apply(new BigDecimal("100.00"), List.of(
                request("UW-SURCHARGE", PremiumAdjustmentType.SURCHARGE_RATE, "0.10"),
                request("CHANNEL-DISCOUNT", PremiumAdjustmentType.DISCOUNT_AMOUNT, "10.00")),
                roundingRule);

        assertEquals(new BigDecimal("100.00"), result.standardPremium());
        assertEquals(new BigDecimal("100.00"), result.totalPremium());
        assertEquals(new BigDecimal("10.00"), result.adjustments().getFirst().adjustmentAmount());
        assertEquals(new BigDecimal("-10.00"), result.adjustments().getLast().adjustmentAmount());
    }

    @Test
    void shouldRejectDiscountRateAboveOne() {
        PricingDomainException exception = assertThrows(PricingDomainException.class, () -> service.apply(
                new BigDecimal("100.00"),
                List.of(request("INVALID", PremiumAdjustmentType.DISCOUNT_RATE, "1.01")),
                roundingRule));

        assertEquals(ProductErrorCode.PRICING_ADJUSTMENT_INVALID.getCode(), exception.getErrorCode());
    }

    @Test
    void shouldRejectAdjustmentThatMakesPremiumNegative() {
        assertThrows(PricingDomainException.class, () -> service.apply(
                new BigDecimal("100.00"),
                List.of(request("INVALID", PremiumAdjustmentType.DISCOUNT_AMOUNT, "100.01")),
                roundingRule));
    }

    private PremiumAdjustmentRequest request(
            String code, PremiumAdjustmentType type, String value) {
        return new PremiumAdjustmentRequest(
                code, type, new BigDecimal(value), "核保结论", "UW-V1");
    }
}
