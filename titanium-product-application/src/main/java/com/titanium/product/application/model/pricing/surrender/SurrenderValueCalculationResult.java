package com.titanium.product.application.model.pricing.surrender;

import java.math.BigDecimal;
import java.util.List;

import com.titanium.product.common.enums.SurrenderRefundBasis;
import com.titanium.product.common.enums.SurrenderRefundType;
import com.titanium.product.pricing.aggregate.lifecycle.PremiumLifecycleAdjustment;
import com.titanium.product.valueobject.pricing.premium.SurrenderFactor;

/** Product 退保价值确认结果及其账务差额事实。 */
public record SurrenderValueCalculationResult(
        String surrenderRequestId,
        String policyCode,
        String policyVersion,
        String policyContentHash,
        Integer policyYear,
        Integer coolingOffDays,
        SurrenderRefundBasis refundBasis,
        SurrenderRefundType refundType,
        boolean withinCoolingOff,
        BigDecimal cashValueRate,
        BigDecimal refundAmount,
        BigDecimal retainedCustomerAmount,
        BigDecimal internalCostRetentionRate,
        List<SurrenderFactor> factors,
        String requestHash,
        String originalResultHash,
        String replacementResultHash,
        String pricingPlanVersion,
        String pricingPlanContentHash,
        PremiumLifecycleAdjustment adjustment) {
}
