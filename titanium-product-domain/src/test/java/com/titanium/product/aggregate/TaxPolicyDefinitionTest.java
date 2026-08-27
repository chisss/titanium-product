package com.titanium.product.aggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.common.enums.TaxPriceMode;
import com.titanium.product.exception.PricingDomainException;

class TaxPolicyDefinitionTest {

    @Test
    void shouldPublishVersionWithStableHash() {
        TaxPolicyDefinition policy = policy(ChargeCategory.TAX, new BigDecimal("0.06"));

        String hash = policy.approve();
        policy.publish();

        assertEquals(64, hash.length());
        assertEquals(ActuarialDefinitionStatus.PUBLISHED, policy.getStatus());
    }

    @Test
    void shouldRejectNonTaxCategory() {
        assertThrows(PricingDomainException.class,
                () -> policy(ChargeCategory.PRODUCT_FEE, new BigDecimal("0.06")));
    }

    @Test
    void shouldRejectRateAboveOne() {
        assertThrows(PricingDomainException.class,
                () -> policy(ChargeCategory.TAX, new BigDecimal("1.01")));
    }

    private TaxPolicyDefinition policy(ChargeCategory category, BigDecimal rate) {
        return TaxPolicyDefinition.createDraft(
                "tax-1", "product-1", "PREMIUM_TAX", "V1", "保费税", "",
                "GLOBAL", category, ChargePayerType.POLICYHOLDER, TaxPriceMode.EXCLUSIVE, rate,
                List.of("BASE_PREMIUM"), "TAX_PAYABLE", "REG-REF-1", null,
                LocalDateTime.of(2026, 1, 1, 0, 0), null, "tenant-1");
    }
}
