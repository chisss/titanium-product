package com.titanium.product.web.controller.pricing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.titanium.common.context.RequestContextInterceptor;
import com.titanium.product.application.command.pricing.RateTableCommandAppService;
import com.titanium.product.application.query.pricing.RateTableQueryAppService;
import com.titanium.product.web.handler.ProductExceptionHandler;
import com.titanium.product.web.mapper.RateTableRowWebMapper;

class ProductRateTableControllerTest {

    private MockMvc mockMvc;
    private RateTableCommandAppService commandAppService;

    @BeforeEach
    void setUp() {
        commandAppService = mock(RateTableCommandAppService.class);
        RateTableQueryAppService queryAppService = mock(RateTableQueryAppService.class);
        ProductRateTableController controller = new ProductRateTableController(
                commandAppService, queryAppService, Mappers.getMapper(RateTableRowWebMapper.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ProductExceptionHandler())
                // m18：显式注册请求上下文拦截器（standaloneSetup 不装配容器里的拦截器），
                // 使 X-Tenant-ID 头经真实路径进入 RequestContextHolder
                .addInterceptors(new RequestContextInterceptor())
                .build();
    }

    @Test
    void shouldCreateRateTableDraftWithStableTenantHeader() throws Exception {
        when(commandAppService.createDraft(any())).thenReturn("table-1");

        mockMvc.perform(post("/web/v1/products/product-1/rate-tables")
                        .header("X-Tenant-ID", "tenant-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tableCode":"RATE-LIFE",
                                  "tableVersion":"V1.0",
                                  "rateUnit":"SUM_INSURED_RATIO",
                                  "currency":"CNY",
                                  "effectiveFrom":"2026-01-01T00:00:00",
                                  "dimensionKeys":["age","gender","paymentTerm","coverageTerm"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("table-1"));
    }

    @Test
    void shouldRejectUnknownRateUnitWithStableBusinessError() throws Exception {
        mockMvc.perform(post("/web/v1/products/product-1/rate-tables")
                        .header("X-Tenant-ID", "tenant-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tableCode":"RATE-LIFE",
                                  "tableVersion":"V1.0",
                                  "rateUnit":"UNKNOWN",
                                  "currency":"CNY",
                                  "effectiveFrom":"2026-01-01T00:00:00",
                                  "dimensionKeys":["age"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("60000104"));
    }
}
