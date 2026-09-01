package com.titanium.product.web.dto.pricing.testcase;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/**
 * 替换定价测试用例请求。
 */
public record ReplacePricingTestCasesDTO(
        @NotEmpty @Valid List<PricingTestCaseDTO> testCases) {
}
