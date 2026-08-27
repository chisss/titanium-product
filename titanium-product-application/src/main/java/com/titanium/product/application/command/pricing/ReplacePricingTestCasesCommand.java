package com.titanium.product.application.command.pricing;

import java.util.List;

/**
 * 整体替换定价测试用例命令。
 */
public record ReplacePricingTestCasesCommand(
        String tenantId,
        String productId,
        String planId,
        List<PricingTestCaseDraft> testCases) {
}
