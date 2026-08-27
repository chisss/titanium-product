package com.titanium.product.web.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.titanium.product.aggregate.PremiumCalculation;
import com.titanium.product.application.command.pricing.PremiumCalculationCommandAppService;
import com.titanium.product.application.query.pricing.PremiumCalculationQueryAppService;
import com.titanium.product.common.enums.PremiumAdjustmentType;
import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.common.enums.PricingCalculationStatus;
import com.titanium.product.valueobject.pricing.PremiumAdjustment;
import com.titanium.product.valueobject.pricing.PremiumCalculationEvidence;
import com.titanium.product.web.handler.ProductExceptionHandler;

class ProductPremiumCalculationApiProviderTest {

    private PremiumCalculationCommandAppService calculationService;
    private PremiumCalculationQueryAppService queryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        calculationService = mock(PremiumCalculationCommandAppService.class);
        queryService = mock(PremiumCalculationQueryAppService.class);
        ProductPremiumCalculationApiProvider provider = new ProductPremiumCalculationApiProvider(
                calculationService, queryService);
        mockMvc = MockMvcBuilders.standaloneSetup(provider)
                .setControllerAdvice(new ProductExceptionHandler())
                .build();
    }

    @Test
    void shouldConfirmAndReturnImmutableCalculationEvidence() throws Exception {
        when(calculationService.confirm(any())).thenReturn(calculation());

        mockMvc.perform(post("/api/v1/products/product-1/premium-calculations")
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.calculationId").value("calculation-1"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.standardPremium").value(1000.00))
                .andExpect(jsonPath("$.data.totalPremium").value(1100.00))
                .andExpect(jsonPath("$.data.adjustments[0].type").value("SURCHARGE_RATE"))
                .andExpect(jsonPath("$.data.pricingPlanVersion").value("P1"));
    }

    @Test
    void shouldQueryCalculationByTenantScopedId() throws Exception {
        when(queryService.get(any())).thenReturn(calculation());

        mockMvc.perform(get("/api/v1/premium-calculations/calculation-1")
                        .header("X-Tenant-ID", "tenant-a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.calculationId").value("calculation-1"))
                .andExpect(jsonPath("$.data.resultHash").value("oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo"));
    }

    private String validRequest() {
        return """
                {
                  "calculationRequestId":"calc-1",
                  "bizNo":"proposal-1",
                  "purpose":"ISSUANCE_CONFIRM",
                  "productVersion":"V1.0",
                  "businessTime":"2026-08-18T12:00:00",
                  "currency":"CNY",
                  "sumInsured":100000,
                  "age":35,
                  "gender":"M",
                  "paymentTermYears":10,
                  "coverageTermYears":20,
                  "paymentPeriods":12,
                  "requestSnapshot":{"insured.age":35},
                  "underwritingAdjustments":[{
                    "adjustmentCode":"UW-SURCHARGE",
                    "type":"SURCHARGE_RATE",
                    "value":0.10,
                    "reason":"核保加费",
                    "ruleVersion":"UW-V1"
                  }]
                }
                """;
    }

    private PremiumCalculation calculation() {
        return PremiumCalculation.restore(
                "calculation-1", "calc-1", "proposal-1", PricingCalculationPurpose.ISSUANCE_CONFIRM,
                PricingCalculationStatus.CONFIRMED, "tenant-a", "product-1",
                LocalDateTime.of(2026, 8, 18, 12, 0), "CNY", new BigDecimal("1000.00"),
                new BigDecimal("1100.00"), new BigDecimal("91.67"), 12,
                List.of(new PremiumAdjustment(
                        "UW-SURCHARGE", PremiumAdjustmentType.SURCHARGE_RATE, new BigDecimal("0.10"),
                        new BigDecimal("100.00"), new BigDecimal("1100.00"), "核保加费", "UW-V1")),
                new PremiumCalculationEvidence(
                        "V1.0", "P1", "pppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppppp",
                        "LIFE_BASE", "V1", "tttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttt",
                        "feature-1", "formula", "V1", "rrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrrr",
                        2, "HALF_UP"),
                Map.of("insured.age", 35),
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "iiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiii",
                "oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo",
                LocalDateTime.of(2026, 8, 18, 12, 1));
    }
}
