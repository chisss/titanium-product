package com.titanium.product.aggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.PricingPlanStatus;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.RateTableRef;
import com.titanium.product.valueobject.pricing.PricingPlanValidationResult;
import com.titanium.product.valueobject.pricing.PricingRoundingRule;
import com.titanium.product.valueobject.pricing.PricingTestCase;
import com.titanium.product.valueobject.pricing.PricingTestCaseResult;

class PricingPlanDefinitionTest {

    @Test
    void shouldApproveAndPublishOnlyAfterAllTestsPass() {
        PricingPlanDefinition plan = draft();
        plan.replaceTestCases(List.of(testCase("BASE", "100.00")));

        String contentHash = plan.approve();
        plan.publish(new PricingPlanValidationResult(
                contentHash, 1, 1,
                List.of(new PricingTestCaseResult(
                        "BASE", true, new BigDecimal("100.00"), new BigDecimal("100.00"),
                        BigDecimal.ZERO, null))));

        assertEquals(PricingPlanStatus.PUBLISHED, plan.status());
        assertEquals(64, plan.contentHash().length());
        assertThrows(PricingDomainException.class, () -> plan.replaceTestCases(List.of()));
    }

    @Test
    void shouldRejectPublishWhenTestCaseFails() {
        PricingPlanDefinition plan = draft();
        plan.replaceTestCases(List.of(testCase("BASE", "100.00")));
        String contentHash = plan.approve();

        PricingDomainException exception = assertThrows(PricingDomainException.class, () -> plan.publish(
                new PricingPlanValidationResult(contentHash, 1, 0, List.of())));

        assertEquals(ProductErrorCode.PRICING_TEST_CASE_FAILED.getCode(), exception.getErrorCode());
    }

    @Test
    void shouldRejectApproveWithoutTestCases() {
        PricingDomainException exception = assertThrows(PricingDomainException.class, draft()::approve);

        assertEquals(ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED.getCode(), exception.getErrorCode());
    }

    @Test
    void shouldProduceStableHashIndependentOfCaseIdsAndSnapshotMapOrder() {
        PricingPlanDefinition first = draft();
        first.replaceTestCases(List.of(testCase("CASE-A", "100.0")));
        PricingPlanDefinition second = draft();
        second.replaceTestCases(List.of(new PricingTestCase(
                "OTHER-ID", "CASE-A", "same", LocalDateTime.of(2026, 1, 1, 0, 0),
                new BigDecimal("100000.00"), 35, "M", 20, 20, 1,
                Map.of("nested", Map.of("b", 2, "a", 1)), new BigDecimal("100.00"), BigDecimal.ZERO)));

        assertEquals(first.approve(), second.approve());
        assertNotEquals(first.testCases().getFirst().caseId(), second.testCases().getFirst().caseId());
    }

    private PricingPlanDefinition draft() {
        return PricingPlanDefinition.createDraft(
                "PLAN-1", "PRODUCT-1", "V1.0", "P1", PricingMode.RATE_TABLE, "cny",
                LocalDateTime.of(2026, 1, 1, 0, 0), null,
                RateTableRef.of(null, "LIFE_BASE", "V1"), null, null,
                new PricingRoundingRule(2, RoundingMode.HALF_UP), "TENANT-1");
    }

    private PricingTestCase testCase(String caseCode, String expectedPremium) {
        return new PricingTestCase(
                "CASE-ID", caseCode, "same", LocalDateTime.of(2026, 1, 1, 0, 0),
                new BigDecimal("100000"), 35, "M", 20, 20, 1,
                Map.of("nested", Map.of("a", 1, "b", 2)), new BigDecimal(expectedPremium), BigDecimal.ZERO);
    }
}
