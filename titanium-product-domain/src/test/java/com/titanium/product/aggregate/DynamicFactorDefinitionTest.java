package com.titanium.product.aggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.common.enums.DynamicFactorMissingPolicy;
import com.titanium.product.common.enums.DynamicFactorSourceType;
import com.titanium.product.common.enums.DynamicFactorTransformType;
import com.titanium.product.common.enums.DynamicFactorValueTimePolicy;
import com.titanium.product.exception.PricingDomainException;

class DynamicFactorDefinitionTest {

    @Test
    void shouldPublishReplayableLinearFactorWithStableHash() {
        DynamicFactorDefinition factor = factor(DynamicFactorMissingPolicy.REJECT, null, true);

        String hash = factor.approve();
        factor.publish();

        assertEquals(64, hash.length());
        assertEquals(ActuarialDefinitionStatus.PUBLISHED, factor.getStatus());
        assertEquals(0, new BigDecimal("1.100").compareTo(factor.transform(new BigDecimal("1.2"))));
    }

    @Test
    void shouldUseDefaultOrSkipMissingFeature() {
        DynamicFactorDefinition defaultFactor = factor(
                DynamicFactorMissingPolicy.USE_DEFAULT, new BigDecimal("1.1"), true);
        DynamicFactorDefinition skipFactor = factor(DynamicFactorMissingPolicy.SKIP, null, true);

        assertEquals(0, new BigDecimal("1.050").compareTo(defaultFactor.transform(null)));
        assertNull(skipFactor.transform(null));
    }

    @Test
    void shouldRejectOutOfRangeAndNonReplayableApproval() {
        DynamicFactorDefinition factor = factor(DynamicFactorMissingPolicy.REJECT, null, true);
        DynamicFactorDefinition nonReplayable = factor(DynamicFactorMissingPolicy.REJECT, null, false);

        assertThrows(PricingDomainException.class, () -> factor.transform(new BigDecimal("2.1")));
        assertThrows(PricingDomainException.class, nonReplayable::approve);
    }

    private DynamicFactorDefinition factor(
            DynamicFactorMissingPolicy missingPolicy, BigDecimal defaultValue, boolean replayable) {
        return DynamicFactorDefinition.createDraft(
                "factor-1", "product-1", "RISK_FACTOR", "V1", "风险因子", "",
                "vehicleRiskScore", "FV1", DynamicFactorSourceType.DERIVED,
                DynamicFactorValueTimePolicy.BUSINESS_TIME, new BigDecimal("0.5"), new BigDecimal("2.0"),
                missingPolicy, defaultValue, DynamicFactorTransformType.LINEAR,
                new BigDecimal("0.5"), new BigDecimal("0.5"), replayable,
                LocalDateTime.of(2026, 1, 1, 0, 0), null, "tenant-1");
    }
}
