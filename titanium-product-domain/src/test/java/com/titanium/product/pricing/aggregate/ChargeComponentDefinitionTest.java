package com.titanium.product.pricing.aggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.common.enums.ChargeCalculationSource;
import com.titanium.product.exception.PricingDomainException;

class ChargeComponentDefinitionTest {

    @Test
    void shouldApprovePublishAndRetireImmutableVersion() {
        ChargeComponentDefinition component = component(
                AmountChannel.CUSTOMER_PRICE, ChargePayerType.POLICYHOLDER);

        String hash = component.approve();
        component.publish();

        assertEquals(64, hash.length());
        assertEquals(ActuarialDefinitionStatus.PUBLISHED, component.getStatus());
        component.retire();
        assertEquals(ActuarialDefinitionStatus.RETIRED, component.getStatus());
    }

    @Test
    void shouldRejectPolicyholderAsInternalCostPayer() {
        assertThrows(PricingDomainException.class, () -> component(
                AmountChannel.INTERNAL_COST, ChargePayerType.POLICYHOLDER));
    }

    private ChargeComponentDefinition component(AmountChannel channel, ChargePayerType payerType) {
        return ChargeComponentDefinition.createDraft(
                "COMPONENT-1", "PRODUCT-1", "BASE_PREMIUM", "V1", "基础保费", "测试费用项",
                ChargeCategory.RISK_PREMIUM, channel, ChargeDirection.DEBIT, payerType,
                ChargeCalculationSource.BASE_PREMIUM, "PREMIUM", true,
                LocalDateTime.of(2026, 1, 1, 0, 0), null, "TENANT-1");
    }
}
