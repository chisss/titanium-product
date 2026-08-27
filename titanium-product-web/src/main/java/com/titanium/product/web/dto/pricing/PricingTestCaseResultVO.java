package com.titanium.product.web.dto.pricing;

import java.math.BigDecimal;

/**
 * 单个测试用例门禁结果响应。
 */
public record PricingTestCaseResultVO(
        String caseCode,
        boolean passed,
        BigDecimal expectedPremium,
        BigDecimal actualPremium,
        BigDecimal difference,
        String failureReason) {
}
