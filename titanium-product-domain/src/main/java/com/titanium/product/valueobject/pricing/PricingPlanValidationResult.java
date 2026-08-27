package com.titanium.product.valueobject.pricing;

import java.util.List;

/**
 * 定价方案发布门禁执行结果。
 */
public record PricingPlanValidationResult(
        String planContentHash,
        int totalCases,
        int passedCases,
        List<PricingTestCaseResult> caseResults) {

    public PricingPlanValidationResult {
        caseResults = caseResults == null ? List.of() : List.copyOf(caseResults);
    }

    public boolean allPassed() {
        return totalCases > 0 && totalCases == passedCases;
    }
}
