package com.titanium.product.web.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.titanium.product.api.request.MaintenancePremiumQuoteRequest;
import com.titanium.product.api.request.MaintenancePremiumQuoteRequest.SnapshotReferenceRequest;
import com.titanium.product.application.command.maintenance.ProductMaintenancePremiumQuoteCommandService;
import com.titanium.product.application.model.pricing.MaintenancePremiumQuoteResult;
import com.titanium.product.web.handler.ProductExceptionHandler;

class ProductMaintenancePremiumQuoteApiProviderTest {

    private ProductMaintenancePremiumQuoteCommandService commandService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        commandService = mock(ProductMaintenancePremiumQuoteCommandService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ProductMaintenancePremiumQuoteApiProvider(commandService))
                .setControllerAdvice(new ProductExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void shouldOpenProductionMaintenanceQuoteRoute() throws Exception {
        LocalDateTime quotedAt = LocalDateTime.parse("2026-08-25T10:00:00");
        when(commandService.quote(any())).thenReturn(new MaintenancePremiumQuoteResult(
                "tenant-1", "case-1", "policy-1", 7L, "product-1", "product-v3", "plan-v2",
                "COVERAGE_AMOUNT_CHANGE", "a".repeat(64), "b".repeat(64), "quote-1",
                "c".repeat(64), "original-calc", "d".repeat(64), "replacement-calc",
                "e".repeat(64), "plan-v2", "f".repeat(64), "idempotency-1",
                "1".repeat(64), "2".repeat(64), "DEBIT 20 CNY; lines=1", "DEBIT",
                new BigDecimal("20"), "CNY", quotedAt, quotedAt.plusHours(24)));

        mockMvc.perform(post("/api/v1/products/product-1/maintenance-premium-quotes")
                        .header("X-Tenant-ID", "tenant-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quoteId").value("quote-1"))
                .andExpect(jsonPath("$.data.amount").value(20))
                .andExpect(jsonPath("$.data.validUntil").exists());
    }

    private MaintenancePremiumQuoteRequest request() {
        OffsetDateTime capturedAt = OffsetDateTime.parse("2026-08-25T08:00:00+08:00");
        return new MaintenancePremiumQuoteRequest(
                "case-1", "policy-1", 7L, "COVERAGE_AMOUNT_CHANGE", "product-v3", "plan-v2",
                "ENDORSEMENT", new SnapshotReferenceRequest(
                        "before.json", "a".repeat(64), 7L, capturedAt),
                new SnapshotReferenceRequest(
                        "proposed.json", "b".repeat(64), 7L, capturedAt.plusMinutes(5)),
                "original-calc", LocalDateTime.parse("2026-08-25T09:00:00"), "CNY",
                new BigDecimal("500000"), 35, "M", 10, 20, 12,
                Map.of("insured.occupation", "1"), List.of(), "agent", 3,
                "保额增加", "idempotency-1", "1".repeat(64));
    }
}
