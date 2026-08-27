package com.titanium.product.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.application.command.pricing.CreatePricingPlanDraftCommand;
import com.titanium.product.application.command.pricing.PricingPlanCommandAppService;
import com.titanium.product.application.query.pricing.PricingPlanQueryAppService;
import com.titanium.product.valueobject.pricing.PricingPlanValidationResult;
import com.titanium.product.valueobject.pricing.PricingTestCaseResult;
import com.titanium.product.web.handler.ProductExceptionHandler;

class ProductPricingPlanControllerTest {

    private PricingPlanCommandAppService commandAppService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        commandAppService = mock(PricingPlanCommandAppService.class);
        PricingPlanQueryAppService queryAppService = mock(PricingPlanQueryAppService.class);
        ProductPricingPlanController controller = new ProductPricingPlanController(
                commandAppService, queryAppService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ProductExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateRateTablePricingPlanDraft() throws Exception {
        when(commandAppService.createDraft(any())).thenReturn("plan-1");

        mockMvc.perform(post("/web/v1/products/product-1/pricing-plans")
                        .header("X-Tenant-ID", "tenant-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRateTablePlanRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("plan-1"));

        ArgumentCaptor<CreatePricingPlanDraftCommand> captor =
                ArgumentCaptor.forClass(CreatePricingPlanDraftCommand.class);
        verify(commandAppService).createDraft(captor.capture());
        assertEquals("tenant-1", captor.getValue().tenantId());
        assertEquals("product-1", captor.getValue().productId());
        assertEquals("V1.0", captor.getValue().productVersion());
        assertEquals(PricingMode.RATE_TABLE, captor.getValue().mode());
        assertEquals("LIFE_BASE", captor.getValue().rateTableRef().tableCode());
        assertEquals("V1", captor.getValue().rateTableRef().version());
    }

    @Test
    void shouldReturnPublishingValidationEvidence() throws Exception {
        when(commandAppService.publish("tenant-1", "product-1", "plan-1"))
                .thenReturn(new PricingPlanValidationResult(
                        "plan-hash", 1, 1, List.of(new PricingTestCaseResult(
                                "BASE", true, new BigDecimal("100.00"),
                                new BigDecimal("100.00"), BigDecimal.ZERO, null))));

        mockMvc.perform(post("/web/v1/products/product-1/pricing-plans/plan-1/publish")
                        .header("X-Tenant-ID", "tenant-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planContentHash").value("plan-hash"))
                .andExpect(jsonPath("$.data.totalCases").value(1))
                .andExpect(jsonPath("$.data.passedCases").value(1))
                .andExpect(jsonPath("$.data.caseResults[0].caseCode").value("BASE"))
                .andExpect(jsonPath("$.data.caseResults[0].passed").value(true));
    }

    @Test
    void shouldRejectUnknownPricingModeWithStableBusinessError() throws Exception {
        mockMvc.perform(post("/web/v1/products/product-1/pricing-plans")
                        .header("X-Tenant-ID", "tenant-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRateTablePlanRequest().replace("RATE_TABLE", "UNKNOWN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ProductErrorCode.PRICING_INPUT_INVALID.getCode()));
    }

    private String validRateTablePlanRequest() {
        return """
                {
                  "productVersion":"V1.0",
                  "planVersion":"P1",
                  "pricingMode":"RATE_TABLE",
                  "currency":"CNY",
                  "effectiveFrom":"2026-09-01T00:00:00",
                  "rateTableCode":"LIFE_BASE",
                  "rateTableVersion":"V1",
                  "rateDimensionKeys":["age","gender"],
                  "roundingScale":2,
                  "roundingMode":"HALF_UP"
                }
                """;
    }
}
