package com.titanium.product.web.dto.pricing;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定价方案后台响应。
 */
public record PricingPlanVO(
        String planId,
        String productId,
        String productVersion,
        String planVersion,
        String pricingMode,
        String status,
        String currency,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        String rateTableCode,
        String rateTableVersion,
        String artifactCode,
        String artifactVersion,
        String inputSchemaVersion,
        String artifactHash,
        String calculationModelCode,
        String calculationModelVersion,
        String calculationModelHash,
        int roundingScale,
        String roundingMode,
        String contentHash,
        List<PricingTestCaseVO> testCases,
        List<TaxPolicyRefDTO> taxPolicyRefs,
        List<CommissionSchemeRefDTO> commissionSchemeRefs,
        List<DynamicFactorRefDTO> dynamicFactorRefs) {
}
