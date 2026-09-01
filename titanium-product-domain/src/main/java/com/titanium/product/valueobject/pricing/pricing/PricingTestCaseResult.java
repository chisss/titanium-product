package com.titanium.product.valueobject.pricing.pricing;

import java.math.BigDecimal;

/**
 * 单个定价发布回归用例的执行结果。
 */
public record PricingTestCaseResult(
        String caseCode,
        boolean passed,
        BigDecimal expectedPremium,
        BigDecimal actualPremium,
        BigDecimal difference,
        String failureReason) {
}
