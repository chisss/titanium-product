package com.titanium.product.web.dto.pricing;

import java.util.List;

/**
 * 定价方案发布门禁响应。
 */
public record PricingPlanValidationVO(
        String planContentHash,
        int totalCases,
        int passedCases,
        List<PricingTestCaseResultVO> caseResults) {
}
