package com.titanium.product.pricing.aggregate.surrender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.titanium.product.common.enums.SurrenderRefundType;
import com.titanium.product.valueobject.pricing.premium.SurrenderValueOutcome;

class SurrenderValuePolicyTest {

    @Test
    void shouldRefundFullCustomerPayableOnLastCoolingOffDay() {
        SurrenderValuePolicy policy = publishedPolicy();
        LocalDate effectiveDate = LocalDate.of(2026, 8, 1);

        SurrenderValueOutcome outcome = policy.calculate(
                new BigDecimal("121.20"), effectiveDate, effectiveDate.plusDays(15), 2, RoundingMode.HALF_UP);

        assertTrue(outcome.withinCoolingOff());
        assertEquals(SurrenderRefundType.COOLING_OFF_FULL_REFUND, outcome.refundType());
        assertEquals(new BigDecimal("121.20"), outcome.refundAmount());
        assertEquals(new BigDecimal("0.00"), outcome.retainedCustomerAmount());
    }

    @Test
    void shouldApplyPublishedCashValueRateOutsideCoolingOff() {
        SurrenderValuePolicy policy = publishedPolicy();
        LocalDate effectiveDate = LocalDate.of(2026, 1, 1);

        SurrenderValueOutcome outcome = policy.calculate(
                new BigDecimal("121.20"), effectiveDate, LocalDate.of(2026, 8, 20), 2, RoundingMode.HALF_UP);

        assertFalse(outcome.withinCoolingOff());
        assertEquals(SurrenderRefundType.CASH_VALUE, outcome.refundType());
        assertEquals(new BigDecimal("72.72"), outcome.refundAmount());
        assertEquals(new BigDecimal("48.48"), outcome.retainedCustomerAmount());
        assertEquals(0, outcome.internalCostRetentionRate().compareTo(BigDecimal.ZERO));
    }

    private SurrenderValuePolicy publishedPolicy() {
        SurrenderValuePolicy policy = SurrenderValuePolicy.createDraft(
                "policy-1", "product-1", "LIFE-SURRENDER-CASH-VALUE", "V1.0", 1, 15,
                new BigDecimal("0.60000000"), BigDecimal.ZERO,
                LocalDateTime.of(2026, 1, 1, 0, 0), null, "1");
        policy.approve();
        policy.publish();
        return policy;
    }
}
