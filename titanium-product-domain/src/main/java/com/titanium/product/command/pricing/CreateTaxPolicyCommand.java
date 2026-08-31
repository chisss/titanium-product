package com.titanium.product.command.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.product.common.enums.TaxPriceMode;

/**
 * 创建税费策略草稿命令。
 */
public record CreateTaxPolicyCommand(
        String tenantId,
        String productId,
        String policyCode,
        String policyVersion,
        String policyName,
        String description,
        String jurisdictionCode,
        ChargeCategory category,
        ChargePayerType payerType,
        TaxPriceMode priceMode,
        BigDecimal taxRate,
        List<String> baseComponentCodes,
        String accountingClass,
        String regulatoryReferenceId,
        String exemptionFeatureCode,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo) {
}
