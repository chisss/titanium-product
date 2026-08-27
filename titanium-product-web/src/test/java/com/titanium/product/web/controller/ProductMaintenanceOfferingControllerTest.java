package com.titanium.product.web.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.titanium.product.application.command.maintenance.ProductMaintenanceOfferingCommandService;
import com.titanium.product.application.query.maintenance.ProductMaintenanceOfferingQueryService;
import com.titanium.product.maintenance.aggregate.ProductMaintenanceOffering;
import com.titanium.product.web.handler.ProductExceptionHandler;

class ProductMaintenanceOfferingControllerTest {

    private ProductMaintenanceOfferingCommandService commandService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        commandService = mock(ProductMaintenanceOfferingCommandService.class);
        ProductMaintenanceOfferingQueryService queryService =
                mock(ProductMaintenanceOfferingQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ProductMaintenanceOfferingController(commandService, queryService))
                .setControllerAdvice(new ProductExceptionHandler())
                .build();
    }

    @Test
    void shouldRetirePublishedOffering() throws Exception {
        ProductMaintenanceOffering offering = ProductMaintenanceOffering.createDraft(
                "offering-1", "tenant-1", "product-1", "product-v1", "plan-v1", "offering-v1",
                LocalDateTime.of(2026, 8, 1, 0, 0), null,
                Set.of("EFFECTIVE"), Set.of("API"), Set.of("POLICY_INFO_CHANGE"));
        offering.publish();
        offering.retire();
        when(commandService.retire("tenant-1", "product-1", "offering-1"))
                .thenReturn(offering);

        mockMvc.perform(post("/web/v1/products/product-1/maintenance-offerings/offering-1/retire")
                        .header("X-Tenant-Id", "tenant-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RETIRED"));

        verify(commandService).retire("tenant-1", "product-1", "offering-1");
    }
}
