package com.titanium.product.application.model.pricing.surrender;

import java.math.BigDecimal;

import com.titanium.product.aggregate.lifecycle.PremiumLifecycleAdjustment;
import com.titanium.product.common.enums.SurrenderRefundType;

/** Product 退保价值确认结果及其账务差额事实。 */
public record SurrenderValueCalculationResult(
        String surrenderRequestId,
        String policyCode,
        String policyVersion,
        String policyContentHash,
        Integer policyYear,
        Integer coolingOffDays,
        SurrenderRefundType refundType,
        boolean withinCoolingOff,
        BigDecimal cashValueRate,
        BigDecimal refundAmount,
        BigDecimal retainedCustomerAmount,
        BigDecimal internalCostRetentionRate,
        String requestHash,
        String originalResultHash,
        String replacementResultHash,
        String pricingPlanVersion,
        String pricingPlanContentHash,
        PremiumLifecycleAdjustment adjustment) {
}
