package com.titanium.product.api.response.premium;

import java.math.BigDecimal;

/** Product 不可变退保价值和生命周期贷项结果。 */
public record SurrenderValueCalculationResponse(
        String surrenderRequestId,
        String policyCode,
        String policyVersion,
        String policyContentHash,
        Integer policyYear,
        Integer coolingOffDays,
        String refundType,
        boolean withinCoolingOff,
        BigDecimal cashValueRate,
        BigDecimal refundAmount,
        BigDecimal retainedCustomerAmount,
        BigDecimal internalCostRetentionRate,
        String originalCalculationId,
        String originalResultHash,
        String replacementCalculationId,
        String replacementResultHash,
        String adjustmentId,
        String requestHash,
        String adjustmentResultHash,
        String pricingPlanVersion,
        String pricingPlanContentHash,
        String direction,
        BigDecimal amount,
        String currency) {
}
