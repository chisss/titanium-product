package com.titanium.product.command.pricing;

import java.util.List;

import com.titanium.product.valueobject.pricing.PricingTestCaseDraft;

/**
 * 整体替换定价测试用例命令。
 */
public record ReplacePricingTestCasesCommand(
        String tenantId,
        String productId,
        String planId,
        List<PricingTestCaseDraft> testCases) {
}
