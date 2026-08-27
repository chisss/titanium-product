package com.titanium.product.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.PremiumAdjustmentType;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.pricing.PremiumAdjustment;
import com.titanium.product.valueobject.pricing.PremiumAdjustmentRequest;
import com.titanium.product.valueobject.pricing.PremiumAdjustmentResult;
import com.titanium.product.valueobject.pricing.PricingRoundingRule;

/**
 * 按顺序应用核保加费和折扣的纯领域服务。
 */
public class PremiumAdjustmentService {

    public PremiumAdjustmentResult apply(
            BigDecimal standardPremium,
            List<PremiumAdjustmentRequest> requests,
            PricingRoundingRule roundingRule) {
        if (standardPremium == null || standardPremium.signum() < 0 || roundingRule == null) {
            throw invalid("标准保费或舍入规则不合法");
        }
        List<PremiumAdjustmentRequest> safeRequests = requests == null ? List.of() : List.copyOf(requests);
        validateRequests(safeRequests);

        BigDecimal normalizedStandard = round(standardPremium, roundingRule);
        BigDecimal current = normalizedStandard;
        List<PremiumAdjustment> applied = new ArrayList<>();
        for (PremiumAdjustmentRequest request : safeRequests) {
            BigDecimal amount = adjustmentAmount(current, request, roundingRule);
            BigDecimal next = round(current.add(amount), roundingRule);
            if (next.signum() < 0) {
                throw invalid("调整项导致保费小于零: " + request.adjustmentCode());
            }
            applied.add(new PremiumAdjustment(
                    request.adjustmentCode(), request.type(), request.value(), amount, next,
                    request.reason(), request.ruleVersion()));
            current = next;
        }
        return new PremiumAdjustmentResult(normalizedStandard, current, applied);
    }

    private void validateRequests(List<PremiumAdjustmentRequest> requests) {
        Set<String> codes = new HashSet<>();
        for (PremiumAdjustmentRequest request : requests) {
            if (request == null || request.adjustmentCode() == null || request.adjustmentCode().isBlank()
                    || request.type() == null || request.value() == null || request.value().signum() < 0) {
                throw invalid("调整项编码、类型和值不能为空，且值不能为负数");
            }
            if (!codes.add(request.adjustmentCode())) {
                throw invalid("调整项编码不能重复: " + request.adjustmentCode());
            }
            if (request.type() == PremiumAdjustmentType.DISCOUNT_RATE
                    && request.value().compareTo(BigDecimal.ONE) > 0) {
                throw invalid("比例折扣不能大于1: " + request.adjustmentCode());
            }
        }
    }

    private BigDecimal adjustmentAmount(
            BigDecimal current, PremiumAdjustmentRequest request, PricingRoundingRule roundingRule) {
        BigDecimal unsigned = switch (request.type()) {
            case SURCHARGE_RATE, DISCOUNT_RATE -> current.multiply(request.value());
            case SURCHARGE_AMOUNT, DISCOUNT_AMOUNT -> request.value();
        };
        BigDecimal rounded = round(unsigned, roundingRule);
        return switch (request.type()) {
            case SURCHARGE_RATE, SURCHARGE_AMOUNT -> rounded;
            case DISCOUNT_RATE, DISCOUNT_AMOUNT -> rounded.negate();
        };
    }

    private BigDecimal round(BigDecimal value, PricingRoundingRule rule) {
        return value.setScale(rule.scale(), rule.roundingMode());
    }

    private PricingDomainException invalid(String detail) {
        return new PricingDomainException(ProductErrorCode.PRICING_ADJUSTMENT_INVALID, detail);
    }
}
