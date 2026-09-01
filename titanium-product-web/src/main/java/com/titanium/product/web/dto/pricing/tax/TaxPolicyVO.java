package com.titanium.product.web.dto.pricing.tax;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 税费策略后台响应。
 */
public record TaxPolicyVO(
        String policyId,
        String productId,
        String policyCode,
        String policyVersion,
        String policyName,
        String description,
        String jurisdictionCode,
        String category,
        String payerType,
        String priceMode,
        BigDecimal taxRate,
        List<String> baseComponentCodes,
        String accountingClass,
        String regulatoryReferenceId,
        String exemptionFeatureCode,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        String status,
        String contentHash) {
}
