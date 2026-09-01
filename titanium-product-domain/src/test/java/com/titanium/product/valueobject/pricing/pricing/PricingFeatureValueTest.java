package com.titanium.product.valueobject.pricing.pricing;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.titanium.product.common.enums.PricingFeatureDataType;

class PricingFeatureValueTest {

    @Test
    void shouldRejectResolvedValueWithMultipleTypedFields() {
        assertThrows(IllegalArgumentException.class, () -> new PricingFeatureValue(
                "insured.age",
                PricingFeatureDataType.INTEGER,
                "RESOLVED",
                "REQUEST",
                "age:3",
                null,
                null,
                "35",
                35L,
                null,
                null,
                null,
                null,
                null,
                null));
    }
}
