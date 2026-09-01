package com.titanium.product.pricing.aggregate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.pricing.premium.PremiumCalculationEvidence;

class PremiumCalculationTest {

    @Test
    void shouldAcceptSameIdempotentRequestAndRejectDifferentRequest() {
        PremiumCalculation calculation = calculation();

        assertDoesNotThrow(() -> calculation.assertSameRequest(hash('a')));
        PricingDomainException exception = assertThrows(
                PricingDomainException.class,
                () -> calculation.assertSameRequest(hash('b')));

        assertEquals(ProductErrorCode.PRICING_IDEMPOTENCY_CONFLICT.getCode(), exception.getErrorCode());
    }

    private PremiumCalculation calculation() {
        return PremiumCalculation.confirm(
                "calculation-1", "request-1", "proposal-1",
                PricingCalculationPurpose.ISSUANCE_CONFIRM, "tenant-1", "product-1",
                LocalDateTime.of(2026, 8, 18, 12, 0), "CNY",
                new BigDecimal("100.00"), new BigDecimal("100.00"),
                new BigDecimal("100.00"), 1, List.of(),
                new PremiumCalculationEvidence(
                        "V1.0", "P1", hash('p'), "LIFE_BASE", "V1", hash('t'),
                        "feature-1", "formula", "V1", hash('r'), 2, "HALF_UP"),
                Map.of("age", 35), hash('a'), hash('i'), hash('o'),
                LocalDateTime.of(2026, 8, 18, 12, 1));
    }

    private String hash(char value) {
        return String.valueOf(value).repeat(64);
    }
}
