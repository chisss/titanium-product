package com.titanium.product.web.dto.pricing.pricingplan;
import java.util.List;

import com.titanium.product.web.dto.pricing.testcase.PricingTestCaseResultVO;

/**
 * 定价方案发布门禁响应。
 */
public record PricingPlanValidationVO(
        String planContentHash,
        int totalCases,
        int passedCases,
        List<PricingTestCaseResultVO> caseResults) {
}
