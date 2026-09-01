package com.titanium.product.application.query.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.titanium.product.common.enums.PricingPlanStatus;
import com.titanium.product.maintenance.aggregate.ProductMaintenanceOffering;
import com.titanium.product.maintenance.repository.ProductMaintenanceOfferingRepository;
import com.titanium.product.pricing.aggregate.PricingPlanDefinition;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.service.ProductQueryService;
import com.titanium.product.repository.PricingPlanRepository;

class ProductMaintenanceOfferingQueryServiceTest {

    private final ProductQueryService productQueryService = mock(ProductQueryService.class);
    private final PricingPlanRepository pricingPlanRepository = mock(PricingPlanRepository.class);
    private final ProductMaintenanceOfferingRepository offeringRepository =
            mock(ProductMaintenanceOfferingRepository.class);

    private ProductMaintenanceOfferingQueryService service;

    @BeforeEach
    void setUp() {
        service = new ProductMaintenanceOfferingQueryService(
                productQueryService, pricingPlanRepository, offeringRepository);
    }

    @Test
    void shouldResolvePublishedOfferingForBusinessContext() {
        ProductMaintenanceOffering offering = draft();
        offering.publish();
        LocalDateTime businessTime = LocalDateTime.of(2026, 8, 24, 12, 0);
        allowProductAndPlan(PricingPlanStatus.PUBLISHED);
        when(offeringRepository.findEffective(
                "tenant-1", "product-1", "product-v3", "plan-v2", businessTime))
                .thenReturn(Optional.of(offering));

        ProductMaintenanceOffering resolved = service.resolve(
                "tenant-1", "product-1", "product-v3", "plan-v2",
                "EFFECTIVE", "API", businessTime);

        assertEquals("offering-1", resolved.offeringId());
    }

    private void allowProductAndPlan(PricingPlanStatus status) {
        ProductQueryResult product = mock(ProductQueryResult.class);
        PricingPlanDefinition plan = mock(PricingPlanDefinition.class);
        when(product.getVersion()).thenReturn("product-v3");
        when(productQueryService.findProductById("product-1", "tenant-1")).thenReturn(product);
        when(plan.productVersion()).thenReturn("product-v3");
        when(plan.status()).thenReturn(status);
        when(pricingPlanRepository.findByVersion("tenant-1", "product-1", "plan-v2"))
                .thenReturn(Optional.of(plan));
    }

    private ProductMaintenanceOffering draft() {
        return ProductMaintenanceOffering.createDraft(
                "offering-1", "tenant-1", "product-1", "product-v3", "plan-v2", "offering-v1",
                LocalDateTime.of(2026, 8, 1, 0, 0), null,
                Set.of("EFFECTIVE"), Set.of("API", "MANUAL"), Set.of("POLICY_INFO_CHANGE"));
    }
}
