package com.titanium.product.web.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.titanium.product.application.query.maintenance.ProductMaintenanceOfferingQueryService;
import com.titanium.product.common.enums.ProductMaintenanceOfferingFailureReason;
import com.titanium.product.exception.ProductMaintenanceOfferingException;
import com.titanium.product.maintenance.aggregate.ProductMaintenanceOffering;
import com.titanium.product.web.handler.ProductExceptionHandler;

class ProductMaintenanceOfferingApiProviderTest {

    private ProductMaintenanceOfferingQueryService queryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        queryService = mock(ProductMaintenanceOfferingQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductMaintenanceOfferingApiProvider(queryService))
                .setControllerAdvice(new ProductExceptionHandler())
                .build();
    }

    @Test
    void shouldResolveVersionedOfferingEvidence() throws Exception {
        ProductMaintenanceOffering offering = offering();
        offering.publish();
        when(queryService.resolve(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(offering);

        mockMvc.perform(get("/api/v1/products/product-1/maintenance-offering")
                        .header("X-Tenant-Id", "tenant-1")
                        .param("productVersion", "product-v3")
                        .param("planVersion", "plan-v2")
                        .param("policyStatus", "EFFECTIVE")
                        .param("source", "API")
                        .param("businessEffectiveAt", "2026-08-24T12:00:00+08:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offeringId").value("offering-1"))
                .andExpect(jsonPath("$.data.offeringVersion").value("offering-v1"))
                .andExpect(jsonPath("$.data.contentHash").value(offering.contentHash()))
                .andExpect(jsonPath("$.data.allowedItemCodes[0]").value("POLICY_INFO_CHANGE"));
    }

    @Test
    void shouldReturnStableNotFoundContract() throws Exception {
        when(queryService.resolve(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new ProductMaintenanceOfferingException(
                        ProductMaintenanceOfferingFailureReason.NOT_FOUND, "Offering不存在"));

        mockMvc.perform(get("/api/v1/products/product-1/maintenance-offering")
                        .header("X-Tenant-Id", "tenant-1")
                        .param("productVersion", "product-v3")
                        .param("planVersion", "plan-v2")
                        .param("policyStatus", "EFFECTIVE")
                        .param("source", "API")
                        .param("businessEffectiveAt", "2026-08-24T12:00:00+08:00"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("60000200"));
    }

    private ProductMaintenanceOffering offering() {
        return ProductMaintenanceOffering.createDraft(
                "offering-1", "tenant-1", "product-1", "product-v3", "plan-v2", "offering-v1",
                LocalDateTime.of(2026, 8, 1, 0, 0), null,
                Set.of("EFFECTIVE"), Set.of("API"), Set.of("POLICY_INFO_CHANGE"));
    }
}
