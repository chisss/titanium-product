package com.titanium.product.application.command.pricing;

import java.time.LocalDateTime;

import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.product.common.enums.ChargeCalculationSource;

/**
 * 创建费用项版本草稿命令。
 */
public record CreateChargeComponentCommand(
        String tenantId,
        String productId,
        String componentCode,
        String componentVersion,
        String componentName,
        String description,
        ChargeCategory category,
        AmountChannel amountChannel,
        ChargeDirection direction,
        ChargePayerType payerType,
        ChargeCalculationSource calculationSource,
        String accountingClass,
        boolean customerVisible,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo) {
}
