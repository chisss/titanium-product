package com.titanium.product.maintenance.aggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.titanium.product.common.enums.ProductMaintenanceOfferingFailureReason;
import com.titanium.product.exception.ProductMaintenanceOfferingException;

class ProductMaintenanceOfferingTest {

    @Test
    void shouldPublishWithStableContentHashAndResolveApplicableContext() {
        ProductMaintenanceOffering first = draft(Set.of("BENEFICIARY_CHANGE", "POLICY_INFO_CHANGE"));
        ProductMaintenanceOffering second = draft(Set.of("POLICY_INFO_CHANGE", "BENEFICIARY_CHANGE"));

        String firstHash = first.publish();
        String secondHash = second.publish();

        assertEquals(firstHash, secondHash);
        assertEquals(64, firstHash.length());
        assertTrue(first.appliesTo(
                "EFFECTIVE", "API", LocalDateTime.of(2026, 8, 24, 12, 0)));
        assertFalse(first.appliesTo(
                "TERMINATED", "API", LocalDateTime.of(2026, 8, 24, 12, 0)));
    }

    @Test
    void shouldIncludeOfferingContentInHash() {
        ProductMaintenanceOffering first = draft(Set.of("POLICY_INFO_CHANGE"));
        ProductMaintenanceOffering second = draft(Set.of("BENEFICIARY_CHANGE"));

        assertNotEquals(first.publish(), second.publish());
    }

    @Test
    void shouldRejectPublishingTwice() {
        ProductMaintenanceOffering offering = draft(Set.of("POLICY_INFO_CHANGE"));
        offering.publish();

        ProductMaintenanceOfferingException exception = assertThrows(
                ProductMaintenanceOfferingException.class, offering::publish);

        assertEquals(ProductMaintenanceOfferingFailureReason.STATE_INVALID, exception.getReason());
    }

    @Test
    void shouldRejectInvalidItemCode() {
        ProductMaintenanceOfferingException exception = assertThrows(
                ProductMaintenanceOfferingException.class,
                () -> draft(Set.of("policy.info.change")));

        assertEquals(ProductMaintenanceOfferingFailureReason.CONTRACT_INVALID, exception.getReason());
    }

    private ProductMaintenanceOffering draft(Set<String> itemCodes) {
        return ProductMaintenanceOffering.createDraft(
                "offering-1", "tenant-1", "product-1", "product-v3", "plan-v2", "offering-v1",
                LocalDateTime.of(2026, 8, 1, 0, 0), null,
                Set.of("EFFECTIVE"), Set.of("API", "MANUAL"), itemCodes);
    }
}
