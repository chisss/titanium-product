package com.titanium.product.application.orchestration.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.enums.product.ProductEnum;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.aggregate.PricingPlanDefinition;
import com.titanium.product.common.enums.RateUnit;
import com.titanium.product.port.PricingPlanRepository;
import com.titanium.product.port.RateTableSnapshotRepository;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.service.ProductQueryService;
import com.titanium.product.service.CalculationModelExecutionService;
import com.titanium.product.service.CalculationTotalsService;
import com.titanium.product.service.PremiumCompositionService;
import com.titanium.product.service.RateTableMatchingService;
import com.titanium.product.valueobject.PremiumQuote;
import com.titanium.product.valueobject.RateTableCriteria;
import com.titanium.product.valueobject.RateTableRef;
import com.titanium.product.valueobject.RateTableRow;
import com.titanium.product.valueobject.RateTableSnapshot;
import com.titanium.product.valueobject.pricing.PricingRoundingRule;

class PremiumQuoteApplicationServiceTest {

    private static final LocalDateTime BUSINESS_TIME = LocalDateTime.of(2026, 8, 18, 12, 0);

    private ProductQueryService productQueryService;
    private PricingPlanRepository pricingPlanRepository;
    private PricingPlanCalculator pricingPlanCalculator;
    private RateTableSnapshotRepository rateTableRepository;
    private PremiumQuoteApplicationService service;

    @BeforeEach
    void setUp() {
        productQueryService = mock(ProductQueryService.class);
        pricingPlanRepository = mock(PricingPlanRepository.class);
        pricingPlanCalculator = mock(PricingPlanCalculator.class);
        rateTableRepository = mock(RateTableSnapshotRepository.class);
        service = new PremiumQuoteApplicationService(
                productQueryService, pricingPlanRepository, pricingPlanCalculator, rateTableRepository,
                new RateTableMatchingService(), new PremiumCompositionService(), new PricingEvidenceHasher(),
                new CalculationModelExecutionService(new CalculationTotalsService()));
    }

    @Test
    void shouldQuoteWithPublishedPricingPlanAndReturnVersionEvidence() {
        PremiumQuoteCommand command = command("CNY");
        ProductQueryResult product = effectiveProduct();
        PricingPlanDefinition plan = pricingPlan("V1.0");
        when(productQueryService.findProductById("product-1", "tenant-a")).thenReturn(product);
        when(pricingPlanRepository.findEffective("tenant-a", "product-1", "CNY", BUSINESS_TIME))
                .thenReturn(Optional.of(plan));
        when(pricingPlanCalculator.calculatePublished(plan, new PricingCalculationInput(
                "tenant-a", "product-1", "request-1", BUSINESS_TIME, "CNY", new BigDecimal("100000"),
                35, "M", 10, 20, 12, Map.of()), false))
                .thenReturn(new PricingCalculationOutcome(
                        new BigDecimal("1000.00"), new BigDecimal("0.01000000"), "row-1",
                        "LIFE_BASE", "V1", "table-hash", "feature-snapshot-1",
                        "premium-formula", "V3", "artifact-hash"));

        PremiumQuote quote = service.quote(command);

        assertEquals(new BigDecimal("1000.00"), quote.totalPremium());
        assertEquals("P1", quote.pricingPlanVersion());
        assertEquals("plan-hash", quote.pricingPlanContentHash());
        assertEquals("feature-snapshot-1", quote.featureSnapshotId());
        assertEquals("V3", quote.ruleArtifactVersion());
        assertEquals("artifact-hash", quote.ruleArtifactHash());
    }

    @Test
    void shouldRejectPricingPlanBoundToAnotherProductVersion() {
        PricingPlanDefinition plan = pricingPlan("V2.0");
        when(productQueryService.findProductById("product-1", "tenant-a")).thenReturn(effectiveProduct());
        when(pricingPlanRepository.findEffective("tenant-a", "product-1", "CNY", BUSINESS_TIME))
                .thenReturn(Optional.of(plan));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.quote(command("CNY")));

        assertEquals(ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED.getCode(), exception.getErrorCode());
        verifyNoInteractions(pricingPlanCalculator);
    }

    @Test
    void shouldQuoteWithTenantScopedProductAndPublishedRateTable() {
        PremiumQuoteCommand command = command("CNY");
        ProductQueryResult product = effectiveProduct();
        RateTableCriteria criteria = new RateTableCriteria(35, "M", 10, 20);
        when(productQueryService.findProductById("product-1", "tenant-a")).thenReturn(product);
        when(rateTableRepository.findEffectiveSnapshot(
                "tenant-a", "product-1", "LIFE_BASE", "V1", BUSINESS_TIME, criteria))
                .thenReturn(Optional.of(snapshot("CNY")));

        PremiumQuote quote = service.quote(command);

        assertEquals(new BigDecimal("1000.00"), quote.totalPremium());
        assertEquals(new BigDecimal("83.33"), quote.installmentAmount());
        assertEquals("row-1", quote.matchedRowId());
        assertNotEquals(quote.inputHash(), quote.resultHash());
        verify(productQueryService).findProductById("product-1", "tenant-a");
        verify(rateTableRepository).findEffectiveSnapshot(
                "tenant-a", "product-1", "LIFE_BASE", "V1", BUSINESS_TIME, criteria);
    }

    @Test
    void shouldFailWithoutEffectiveRateTableInsteadOfUsingBaseRate() {
        when(productQueryService.findProductById("product-1", "tenant-a")).thenReturn(effectiveProduct());
        when(rateTableRepository.findEffectiveSnapshot(
                "tenant-a", "product-1", "LIFE_BASE", "V1", BUSINESS_TIME,
                new RateTableCriteria(35, "M", 10, 20)))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.quote(command("CNY")));

        assertEquals(ProductErrorCode.RATE_TABLE_NOT_EFFECTIVE.getCode(), exception.getErrorCode());
    }

    @Test
    void shouldRejectCurrencyMismatch() {
        when(productQueryService.findProductById("product-1", "tenant-a")).thenReturn(effectiveProduct());
        when(rateTableRepository.findEffectiveSnapshot(
                "tenant-a", "product-1", "LIFE_BASE", "V1", BUSINESS_TIME,
                new RateTableCriteria(35, "M", 10, 20)))
                .thenReturn(Optional.of(snapshot("USD")));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.quote(command("CNY")));

        assertEquals(ProductErrorCode.PRICING_CURRENCY_MISMATCH.getCode(), exception.getErrorCode());
    }

    @Test
    void shouldRejectInactiveProductBeforeReadingRateTable() {
        ProductQueryResult product = effectiveProduct();
        product.setStatus(ProductEnum.ProductStatus.DRAFT);
        when(productQueryService.findProductById("product-1", "tenant-a")).thenReturn(product);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.quote(command("CNY")));

        assertEquals(ProductErrorCode.PRICING_PLAN_NOT_EFFECTIVE.getCode(), exception.getErrorCode());
    }

    private PremiumQuoteCommand command(String currency) {
        return new PremiumQuoteCommand(
                "tenant-a", "product-1", "request-1", BUSINESS_TIME, currency,
                new BigDecimal("100000"), 35, "M", 10, 20, 12);
    }

    private ProductQueryResult effectiveProduct() {
        ProductQueryResult product = new ProductQueryResult();
        product.setProductId("product-1");
        product.setVersion("V1.0");
        product.setStatus(ProductEnum.ProductStatus.EFFECTIVE);
        product.setPricingMode(PricingMode.RATE_TABLE);
        product.setRateTableRef(new RateTableRef("legacy-clause", "LIFE_BASE", "V1", List.of(
                "age", "gender", "paymentTerm", "coverageTerm")));
        return product;
    }

    private PricingPlanDefinition pricingPlan(String productVersion) {
        PricingPlanDefinition plan = mock(PricingPlanDefinition.class);
        when(plan.productVersion()).thenReturn(productVersion);
        when(plan.mode()).thenReturn(PricingMode.RATE_TABLE);
        when(plan.planVersion()).thenReturn("P1");
        when(plan.contentHash()).thenReturn("plan-hash");
        when(plan.roundingRule()).thenReturn(new PricingRoundingRule(2, RoundingMode.HALF_UP));
        return plan;
    }

    private RateTableSnapshot snapshot(String currency) {
        RateTableRow row = new RateTableRow(
                "row-1", 18, 61, "M", 10, 20,
                new BigDecimal("0.01000000"), null, null);
        return new RateTableSnapshot(
                "table-1", "product-1", "LIFE_BASE", "V1", RateUnit.SUM_INSURED_RATIO, currency,
                LocalDateTime.of(2026, 1, 1, 0, 0), null, "content-hash", List.of(row));
    }
}
