package com.titanium.product.application.orchestration.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.titanium.product.command.maintenance.CreateProductMaintenanceOfferingCommand;
import com.titanium.product.common.enums.PricingPlanStatus;
import com.titanium.product.common.enums.ProductMaintenanceOfferingFailureReason;
import com.titanium.product.common.enums.ProductMaintenanceOfferingStatus;
import com.titanium.product.exception.ProductMaintenanceOfferingException;
import com.titanium.product.maintenance.aggregate.ProductMaintenanceOffering;
import com.titanium.product.maintenance.repository.ProductMaintenanceOfferingRepository;
import com.titanium.product.pricing.aggregate.PricingPlanDefinition;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.service.ProductQueryService;
import com.titanium.product.repository.PricingPlanRepository;

class ProductMaintenanceOfferingManagementApplicationServiceTest {

    private final ProductQueryService productQueryService = mock(ProductQueryService.class);
    private final PricingPlanRepository pricingPlanRepository = mock(PricingPlanRepository.class);
    private final ProductMaintenanceOfferingRepository offeringRepository =
            mock(ProductMaintenanceOfferingRepository.class);

    private ProductMaintenanceOfferingManagementApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ProductMaintenanceOfferingManagementApplicationService(
                productQueryService, pricingPlanRepository, offeringRepository);
    }

    @Test
    void shouldCreateDraftAfterProductAndPlanVersionValidation() {
        allowProductAndPlan(PricingPlanStatus.DRAFT);

        String offeringId = service.createDraft(command());

        verify(offeringRepository).save(any(ProductMaintenanceOffering.class));
        assertEquals(36, offeringId.length());
    }

    @Test
    void shouldRejectDraftWhenProductVersionDoesNotMatch() {
        ProductQueryResult product = mock(ProductQueryResult.class);
        when(product.getVersion()).thenReturn("product-v2");
        when(productQueryService.findProductById("product-1", "tenant-1")).thenReturn(product);

        ProductMaintenanceOfferingException exception = assertThrows(
                ProductMaintenanceOfferingException.class, () -> service.createDraft(command()));

        assertEquals(ProductMaintenanceOfferingFailureReason.VERSION_MISMATCH, exception.getReason());
        verify(offeringRepository, never()).save(any());
    }

    @Test
    void shouldPublishOnlyAgainstPublishedPricingPlan() {
        ProductMaintenanceOffering offering = draft();
        when(offeringRepository.findById("tenant-1", "product-1", "offering-1"))
                .thenReturn(Optional.of(offering));
        allowProductAndPlan(PricingPlanStatus.DRAFT);

        ProductMaintenanceOfferingException exception = assertThrows(
                ProductMaintenanceOfferingException.class,
                () -> service.publish("tenant-1", "product-1", "offering-1"));

        assertEquals(ProductMaintenanceOfferingFailureReason.VERSION_MISMATCH, exception.getReason());
        verify(offeringRepository, never()).save(any());
    }

    @Test
    void shouldRetirePublishedOffering() {
        ProductMaintenanceOffering offering = draft();
        offering.publish();
        when(offeringRepository.findById("tenant-1", "product-1", "offering-1"))
                .thenReturn(Optional.of(offering));

        ProductMaintenanceOffering retired = service.retire(
                "tenant-1", "product-1", "offering-1");

        assertEquals(ProductMaintenanceOfferingStatus.RETIRED, retired.status());
        verify(offeringRepository).save(offering);
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

    private CreateProductMaintenanceOfferingCommand command() {
        return new CreateProductMaintenanceOfferingCommand(
                "tenant-1", "product-1", "product-v3", "plan-v2", "offering-v1",
                LocalDateTime.of(2026, 8, 1, 0, 0), null,
                Set.of("EFFECTIVE"), Set.of("API", "MANUAL"), Set.of("POLICY_INFO_CHANGE"));
    }

    private ProductMaintenanceOffering draft() {
        return ProductMaintenanceOffering.createDraft(
                "offering-1", "tenant-1", "product-1", "product-v3", "plan-v2", "offering-v1",
                LocalDateTime.of(2026, 8, 1, 0, 0), null,
                Set.of("EFFECTIVE"), Set.of("API", "MANUAL"), Set.of("POLICY_INFO_CHANGE"));
    }
}
