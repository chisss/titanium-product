package com.titanium.product.valueobject.pricing.premium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.exception.PricingDomainException;

class SurrenderPremiumFactsTest {

    @Test
    @DisplayName("UNAVAILABLE：三项均不可得，complete() 为 false（由使用点失败关闭）")
    void shouldReportUnavailableAsIncomplete() {
        assertNull(SurrenderPremiumFacts.UNAVAILABLE.annualPremium());
        assertNull(SurrenderPremiumFacts.UNAVAILABLE.coverageEndDate());
        assertFalse(SurrenderPremiumFacts.UNAVAILABLE.premiumPaidUp());
        assertFalse(SurrenderPremiumFacts.UNAVAILABLE.complete());
    }

    @Test
    @DisplayName("年保费为负即拒绝：负保费会把未满期保费算成负额退款")
    void shouldRejectNegativeAnnualPremium() {
        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> new SurrenderPremiumFacts(new BigDecimal("-1.00"), LocalDate.of(2026, 12, 31), false));

        assertEquals(ProductErrorCode.SURRENDER_FACTOR_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("年保费与保障止期齐备即视为完整，与缴费状态无关")
    void shouldBeCompleteWhenBothAmountAndCoverageEndPresent() {
        SurrenderPremiumFacts facts = new SurrenderPremiumFacts(
                new BigDecimal("3650.00"), LocalDate.of(2026, 12, 31), false);

        assertTrue(facts.complete());
    }

    @Test
    @DisplayName("缴费期满（含趸缴）无需任何事实即为完整：该项由「期满」本身决定为 0")
    void shouldBeCompleteOnPaidUpWithoutAnyFact() {
        assertTrue(new SurrenderPremiumFacts(null, null, true).complete(),
                "已缴清是该项为 0 的充分理由，不因取不到年保费/止期而失败关闭");
    }

    @Test
    @DisplayName("仅有年保费或仅有保障止期均为不完整：半个事实不足以折算")
    void shouldRequireBothFactsWhenNotPaidUp() {
        assertFalse(new SurrenderPremiumFacts(new BigDecimal("3650.00"), null, false).complete());
        assertFalse(new SurrenderPremiumFacts(null, LocalDate.of(2026, 12, 31), false).complete());
    }
}
