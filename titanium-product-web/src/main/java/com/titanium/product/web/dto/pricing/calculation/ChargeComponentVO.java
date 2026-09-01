package com.titanium.product.web.dto.pricing.calculation;

import java.time.LocalDateTime;

/**
 * 费用项后台响应。
 */
public record ChargeComponentVO(
        String componentId,
        String productId,
        String componentCode,
        String componentVersion,
        String componentName,
        String description,
        String category,
        String amountChannel,
        String direction,
        String payerType,
        String calculationSource,
        String accountingClass,
        boolean customerVisible,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        String status,
        String contentHash) {
}
