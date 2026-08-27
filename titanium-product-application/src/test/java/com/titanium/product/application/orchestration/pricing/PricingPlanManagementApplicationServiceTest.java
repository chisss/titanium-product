package com.titanium.product.application.orchestration.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.aggregate.CalculationModelDefinition;
import com.titanium.product.aggregate.PricingPlanDefinition;
import com.titanium.product.application.command.pricing.CreatePricingPlanDraftCommand;
import com.titanium.product.port.CommissionResolutionPort;
import com.titanium.product.port.PricingPlanRepository;
import com.titanium.product.query.result.ProductQueryResult;
import com.titanium.product.query.service.ProductQueryService;
import com.titanium.product.repository.CalculationModelRepository;
import com.titanium.product.repository.DynamicFactorRepository;
import com.titanium.product.repository.TaxPolicyRepository;
import com.titanium.product.valueobject.RateTableRef;
import com.titanium.product.valueobject.pricing.CalculationModelRef;
import com.titanium.product.valueobject.pricing.CommissionSchemeRef;
import com.titanium.product.valueobject.pricing.PricingPlanValidationResult;
import com.titanium.product.valueobject.pricing.PricingRoundingRule;
import com.titanium.product.valueobject.pricing.PricingTestCase;

class PricingPlanManagementApplicationServiceTest {

    private final ProductQueryService productQueryService = mock(ProductQueryService.class);
    private final PricingPlanRepository pricingPlanRepository = mock(PricingPlanRepository.class);
    private final CalculationModelRepository calculationModelRepository = mock(CalculationModelRepository.class);
    private final TaxPolicyRepository taxPolicyRepository = mock(TaxPolicyRepository.class);
    private final DynamicFactorRepository dynamicFactorRepository = mock(DynamicFactorRepository.class);
    private final PricingPlanTestRunner testRunner = mock(PricingPlanTestRunner.class);
    private final CommissionResolutionPort commissionResolutionPort = mock(CommissionResolutionPort.class);
    private PricingPlanManagementApplicationService service;

    @BeforeEach
    void setUp() {
        service = new PricingPlanManagementApplicationService(
                productQueryService, pricingPlanRepository, calculationModelRepository,
                taxPolicyRepository, dynamicFactorRepository, testRunner, commissionResolutionPort);
    }

    @Test
    void shouldCreateDraftWithPublishedCalculationModelRef() {
        LocalDateTime effectiveFrom = LocalDateTime.of(2026, 1, 1, 0, 0);
        String modelHash = "a".repeat(64);
        ProductQueryResult product = mock(ProductQueryResult.class);
        CalculationModelDefinition model = mock(CalculationModelDefinition.class);
        when(product.getVersion()).thenReturn("V1.0");
        when(productQueryService.findProductById("PRODUCT-1", "TENANT-1")).thenReturn(product);
        when(calculationModelRepository.findPublished(
                "TENANT-1", "PRODUCT-1", "LIFE_TOTAL", "V1", effectiveFrom))
                .thenReturn(Optional.of(model));
        when(model.getContentHash()).thenReturn(modelHash);
        when(model.getCurrency()).thenReturn("CNY");

        service.createDraft(command(effectiveFrom, modelHash));

        verify(pricingPlanRepository).save(any(PricingPlanDefinition.class));
    }

    @Test
    void shouldRejectCalculationModelHashMismatch() {
        LocalDateTime effectiveFrom = LocalDateTime.of(2026, 1, 1, 0, 0);
        ProductQueryResult product = mock(ProductQueryResult.class);
        CalculationModelDefinition model = mock(CalculationModelDefinition.class);
        when(product.getVersion()).thenReturn("V1.0");
        when(productQueryService.findProductById("PRODUCT-1", "TENANT-1")).thenReturn(product);
        when(calculationModelRepository.findPublished(
                "TENANT-1", "PRODUCT-1", "LIFE_TOTAL", "V1", effectiveFrom))
                .thenReturn(Optional.of(model));
        when(model.getContentHash()).thenReturn("b".repeat(64));
        when(model.getCurrency()).thenReturn("CNY");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createDraft(command(effectiveFrom, "a".repeat(64))));

        assertEquals(ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED.getCode(), exception.getErrorCode());
        verify(pricingPlanRepository, never()).save(any());
    }

    @Test
    void shouldRejectPublishWhenEffectivePeriodOverlaps() {
        PricingPlanDefinition plan = approvedPlan();
        when(pricingPlanRepository.findById("TENANT-1", "PRODUCT-1", "PLAN-1"))
                .thenReturn(Optional.of(plan));
        when(pricingPlanRepository.existsPublishedOverlap(
                "TENANT-1", "PRODUCT-1", "PLAN-1", "CNY", plan.effectiveFrom(), plan.effectiveTo()))
                .thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.publish("TENANT-1", "PRODUCT-1", "PLAN-1"));

        assertEquals(ProductErrorCode.PRICING_PLAN_EFFECTIVE_PERIOD_CONFLICT.getCode(), exception.getErrorCode());
        verify(testRunner, never()).run(any());
    }

    @Test
    void shouldRevalidateCommissionSchemeBeforePublish() {
        PricingPlanDefinition plan = approvedCommissionPlan();
        PricingPlanValidationResult validation = mock(PricingPlanValidationResult.class);
        when(pricingPlanRepository.findById("TENANT-1", "PRODUCT-1", "PLAN-COMMISSION"))
                .thenReturn(Optional.of(plan));
        when(testRunner.run(plan)).thenReturn(validation);
        when(validation.allPassed()).thenReturn(true);
        when(validation.planContentHash()).thenReturn(plan.contentHash());

        service.publish("TENANT-1", "PRODUCT-1", "PLAN-COMMISSION");

        verify(commissionResolutionPort).validate(argThat(request ->
                request.reference().channelId().equals("CHANNEL-1")
                        && request.reference().schemeCode().equals("AGENT_STANDARD")));
    }

    private PricingPlanDefinition approvedPlan() {
        PricingPlanDefinition plan = PricingPlanDefinition.createDraft(
                "PLAN-1", "PRODUCT-1", "V1.0", "P1", PricingMode.RATE_TABLE, "CNY",
                LocalDateTime.of(2026, 1, 1, 0, 0), null,
                RateTableRef.of(null, "LIFE_BASE", "V1"), null, null,
                new PricingRoundingRule(2, RoundingMode.HALF_UP), "TENANT-1");
        plan.replaceTestCases(List.of(new PricingTestCase(
                "CASE-1", "BASE", null, LocalDateTime.of(2026, 6, 1, 0, 0),
                new BigDecimal("100000"), 35, "M", 20, 20, 1, Map.of(),
                new BigDecimal("150"), BigDecimal.ZERO)));
        plan.approve();
        return plan;
    }

    private PricingPlanDefinition approvedCommissionPlan() {
        PricingPlanDefinition plan = PricingPlanDefinition.createDraft(
                "PLAN-COMMISSION", "PRODUCT-1", "V1.0", "P1", PricingMode.RATE_TABLE, "CNY",
                LocalDateTime.of(2026, 1, 1, 0, 0), null,
                RateTableRef.of(null, "LIFE_BASE", "V1"), null, null, null, List.of(),
                List.of(new CommissionSchemeRef("CHANNEL-1", "AGENT_STANDARD", "V1", "a".repeat(64))),
                new PricingRoundingRule(2, RoundingMode.HALF_UP), "TENANT-1");
        plan.replaceTestCases(List.of(new PricingTestCase(
                "CASE-1", "BASE", null, LocalDateTime.of(2026, 6, 1, 0, 0),
                new BigDecimal("100000"), 35, "M", 20, 20, 1, Map.of("channelId", "CHANNEL-1"),
                new BigDecimal("150"), BigDecimal.ZERO)));
        plan.approve();
        return plan;
    }

    private CreatePricingPlanDraftCommand command(LocalDateTime effectiveFrom, String modelHash) {
        return new CreatePricingPlanDraftCommand(
                "TENANT-1", "PRODUCT-1", "V1.0", "P1", PricingMode.RATE_TABLE, "CNY",
                effectiveFrom, null, RateTableRef.of(null, "LIFE_BASE", "V1"), null, null,
                new CalculationModelRef("LIFE_TOTAL", "V1", modelHash),
                new PricingRoundingRule(2, RoundingMode.HALF_UP));
    }
}
