package com.titanium.product.web.provider;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.application.query.pricing.PremiumPricingQueryAppService;
import com.titanium.product.valueobject.PremiumQuote;
import com.titanium.product.valueobject.pricing.CalculationLine;
import com.titanium.product.valueobject.pricing.CalculationTotals;
import com.titanium.product.web.handler.ProductExceptionHandler;

class ProductPricingApiProviderTest {

    private PremiumPricingQueryAppService pricingQueryAppService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        pricingQueryAppService = mock(PremiumPricingQueryAppService.class);
        ProductPricingApiProvider provider = new ProductPricingApiProvider(pricingQueryAppService);
        mockMvc = MockMvcBuilders.standaloneSetup(provider)
                .setControllerAdvice(new ProductExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnPricingPlanVersionEvidence() throws Exception {
        when(pricingQueryAppService.quote(any())).thenReturn(new PremiumQuote(
                "quote-1", "request-1", "product-1", "V1.0", "CNY", new BigDecimal("1000.00"),
                new BigDecimal("83.33"), 12, new BigDecimal("0.01"), "row-1",
                "LIFE_BASE", "V1", "table-hash", "P1", "plan-hash",
                "feature-snapshot-1", "premium-formula", "V3", "artifact-hash",
                2, "HALF_UP",
                "input-hash", "result-hash"));

        mockMvc.perform(post("/api/v1/products/product-1/premium-quotes")
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pricingPlanVersion").value("P1"))
                .andExpect(jsonPath("$.data.pricingPlanContentHash").value("plan-hash"))
                .andExpect(jsonPath("$.data.featureSnapshotId").value("feature-snapshot-1"))
                .andExpect(jsonPath("$.data.ruleArtifactVersion").value("V3"))
                .andExpect(jsonPath("$.data.ruleArtifactHash").value("artifact-hash"));
    }

    @Test
    void shouldNotExposeInternalCostInQuoteResponse() throws Exception {
        when(pricingQueryAppService.quote(any())).thenReturn(quoteWithInternalCost());

        mockMvc.perform(post("/api/v1/products/product-1/premium-quotes")
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.calculationTotals.customerPayable").value(110.00))
                .andExpect(jsonPath("$.data.calculationTotals.internalCostTotal").value(nullValue()))
                .andExpect(jsonPath("$.data.calculationLines.length()").value(1))
                .andExpect(jsonPath("$.data.calculationLines[0].componentCode").value("BASE_PREMIUM"));
    }

    @Test
    void shouldReturnStableBusinessErrorWhenRateTableIsNotEffective() throws Exception {
        when(pricingQueryAppService.quote(any()))
                .thenThrow(new BusinessException(ProductErrorCode.RATE_TABLE_NOT_EFFECTIVE));

        mockMvc.perform(post("/api/v1/products/product-1/premium-quotes")
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ProductErrorCode.RATE_TABLE_NOT_EFFECTIVE.getCode()))
                .andExpect(jsonPath("$.message").value(ProductErrorCode.RATE_TABLE_NOT_EFFECTIVE.getMessage()));
    }

    @Test
    void shouldRejectInvalidRequestBeforeCallingApplication() throws Exception {
        mockMvc.perform(post("/api/v1/products/product-1/premium-quotes")
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ProductErrorCode.PRICING_INPUT_INVALID.getCode()));
    }

    private String validRequest() {
        return """
                {
                  "requestId": "request-1",
                  "businessTime": "2026-08-18T12:00:00",
                  "currency": "CNY",
                  "sumInsured": 100000,
                  "age": 35,
                  "gender": "M",
                  "paymentTermYears": 10,
                  "coverageTermYears": 20,
                  "paymentPeriods": 12
                }
                """;
    }

    private PremiumQuote quoteWithInternalCost() {
        CalculationLine base = new CalculationLine(
                "line-1", "BASE_PREMIUM", "V1", ChargeCategory.RISK_PREMIUM,
                AmountChannel.CUSTOMER_PRICE, ChargeDirection.DEBIT, ChargePayerType.POLICYHOLDER,
                "PREMIUM", "CNY", new BigDecimal("100.00"), BigDecimal.ONE,
                new BigDecimal("100.00"), "BASE", true, "标准保费");
        CalculationLine internal = new CalculationLine(
                "line-2", "INTERNAL_LOADING", "V1", ChargeCategory.OTHER_INTERNAL_COST,
                AmountChannel.INTERNAL_COST, ChargeDirection.DEBIT, ChargePayerType.INSURER,
                "INTERNAL_COST", "CNY", new BigDecimal("100.00"), new BigDecimal("0.20"),
                new BigDecimal("20.00"), "INTERNAL", false, "内部费用加载");
        return new PremiumQuote(
                "quote-1", "request-1", "product-1", "V1.0", "CNY", new BigDecimal("110.00"),
                new BigDecimal("110.00"), 1, new BigDecimal("0.001"), "row-1",
                "LIFE_BASE", "V1", "table-hash", "P1", "plan-hash",
                "feature-1", null, null, null, 2, "HALF_UP", "input-hash", "result-hash",
                new CalculationTotals(new BigDecimal("100.00"), new BigDecimal("10.00"),
                        new BigDecimal("110.00"), new BigDecimal("20.00")),
                List.of(base, internal), "MODEL", "V1", "model-hash");
    }
}
