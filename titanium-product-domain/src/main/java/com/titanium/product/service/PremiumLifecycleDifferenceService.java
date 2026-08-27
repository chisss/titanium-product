package com.titanium.product.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.aggregate.PremiumCalculation;
import com.titanium.product.common.enums.PremiumBalanceDirection;
import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.pricing.CalculationLine;
import com.titanium.product.valueobject.pricing.lifecycle.PremiumLifecycleDifference;
import com.titanium.product.valueobject.pricing.lifecycle.PremiumLifecycleDifferenceLine;

/**
 * 比较两份不可变确认计算，产出可入账的生命周期差额。
 */
public class PremiumLifecycleDifferenceService {

    public PremiumLifecycleDifference compare(
            PremiumCalculation original, PremiumCalculation replacement) {
        validateCalculations(original, replacement);
        List<PremiumLifecycleDifferenceLine> lines = compareLines(original, replacement);

        BigDecimal customerDelta = replacement.getCalculationTotals().customerPayable()
                .subtract(original.getCalculationTotals().customerPayable());
        BigDecimal taxDelta = replacement.getCalculationTotals().taxAndLevyTotal()
                .subtract(original.getCalculationTotals().taxAndLevyTotal());
        BigDecimal internalDelta = replacement.getCalculationTotals().internalCostTotal()
                .subtract(original.getCalculationTotals().internalCostTotal());
        BigDecimal lineCustomerDelta = lines.stream()
                .filter(PremiumLifecycleDifferenceLine::affectsCustomerPayable)
                .map(PremiumLifecycleDifferenceLine::signedDifference)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (lineCustomerDelta.compareTo(customerDelta) != 0) {
            throw invalid("生命周期客户价格差额明细与汇总无法勾稽");
        }
        return new PremiumLifecycleDifference(
                direction(customerDelta), customerDelta.abs(), direction(taxDelta), taxDelta.abs(),
                direction(internalDelta), internalDelta.abs(), lines);
    }

    private List<PremiumLifecycleDifferenceLine> compareLines(
            PremiumCalculation original, PremiumCalculation replacement) {
        Map<LineKey, LineAmounts> amounts = new LinkedHashMap<>();
        original.getCalculationLines().forEach(line -> amounts
                .computeIfAbsent(LineKey.from(line), ignored -> new LineAmounts())
                .addOriginal(line));
        replacement.getCalculationLines().forEach(line -> amounts
                .computeIfAbsent(LineKey.from(line), ignored -> new LineAmounts())
                .addReplacement(line));

        if (amounts.isEmpty()) {
            return legacyDifferenceLine(original, replacement);
        }

        List<PremiumLifecycleDifferenceLine> differences = new ArrayList<>();
        amounts.forEach((key, value) -> {
            BigDecimal delta = value.replacementSigned.subtract(value.originalSigned);
            if (delta.signum() == 0) {
                return;
            }
            differences.add(new PremiumLifecycleDifferenceLine(
                    stableLineId(key), key.componentCode, value.originalVersion, value.replacementVersion,
                    key.category, key.amountChannel, chargeDirection(delta), key.payerType,
                    key.accountingClass, original.getCurrency(), directionOrNull(value.originalSigned),
                    value.originalSigned.abs(), directionOrNull(value.replacementSigned),
                    value.replacementSigned.abs(), delta.abs(), value.customerVisible,
                    value.affectsCustomerPayable, value.description));
        });
        return List.copyOf(differences);
    }

    private List<PremiumLifecycleDifferenceLine> legacyDifferenceLine(
            PremiumCalculation original, PremiumCalculation replacement) {
        BigDecimal delta = replacement.getTotalPremium().subtract(original.getTotalPremium());
        if (delta.signum() == 0) {
            return List.of();
        }
        return List.of(new PremiumLifecycleDifferenceLine(
                "legacy-customer-payable", "CUSTOMER_PAYABLE", original.getEvidence().productVersion(),
                replacement.getEvidence().productVersion(), ChargeCategory.RISK_PREMIUM,
                AmountChannel.CUSTOMER_PRICE, chargeDirection(delta), ChargePayerType.POLICYHOLDER,
                "PREMIUM", original.getCurrency(), ChargeDirection.DEBIT, original.getTotalPremium(),
                ChargeDirection.DEBIT, replacement.getTotalPremium(), delta.abs(), true, true,
                "兼容历史确认计算的客户应付差额"));
    }

    private void validateCalculations(PremiumCalculation original, PremiumCalculation replacement) {
        if (original == null || replacement == null) {
            throw invalid("原计算和保全重算不能为空");
        }
        if (original.getCalculationId().equals(replacement.getCalculationId())) {
            throw invalid("原计算和保全重算不能相同");
        }
        if (!Objects.equals(original.getTenantId(), replacement.getTenantId())
                || !Objects.equals(original.getProductId(), replacement.getProductId())
                || !Objects.equals(original.getCurrency(), replacement.getCurrency())) {
            throw invalid("生命周期比较必须属于同一租户、产品和币种");
        }
        if (!Objects.equals(original.getBizNo(), replacement.getBizNo())) {
            throw invalid("生命周期比较必须属于同一业务号");
        }
        if (original.getPurpose() != PricingCalculationPurpose.ISSUANCE_CONFIRM
                && original.getPurpose() != PricingCalculationPurpose.MAINTENANCE) {
            throw invalid("原计算必须使用 ISSUANCE_CONFIRM 或 MAINTENANCE 用途");
        }
        if (replacement.getPurpose() != PricingCalculationPurpose.MAINTENANCE) {
            throw invalid("替代计算必须使用 MAINTENANCE 用途");
        }
    }

    private PremiumBalanceDirection direction(BigDecimal value) {
        if (value.signum() > 0) {
            return PremiumBalanceDirection.DEBIT;
        }
        if (value.signum() < 0) {
            return PremiumBalanceDirection.CREDIT;
        }
        return PremiumBalanceDirection.NONE;
    }

    private ChargeDirection chargeDirection(BigDecimal value) {
        return value.signum() > 0 ? ChargeDirection.DEBIT : ChargeDirection.CREDIT;
    }

    private ChargeDirection directionOrNull(BigDecimal value) {
        return value.signum() == 0 ? null : chargeDirection(value);
    }

    private String stableLineId(LineKey key) {
        return UUID.nameUUIDFromBytes(key.canonical().getBytes(StandardCharsets.UTF_8)).toString();
    }

    private PricingDomainException invalid(String detail) {
        return new PricingDomainException(ProductErrorCode.PRICING_PLAN_VALIDATION_FAILED, detail);
    }

    private record LineKey(
            String componentCode,
            ChargeCategory category,
            AmountChannel amountChannel,
            ChargePayerType payerType,
            String accountingClass) {

        private static LineKey from(CalculationLine line) {
            return new LineKey(line.componentCode(), line.category(), line.amountChannel(),
                    line.payerType(), line.accountingClass());
        }

        private String canonical() {
            return String.join("|", componentCode, category.getCode(), amountChannel.getCode(),
                    payerType.getCode(), accountingClass);
        }
    }

    private static final class LineAmounts {
        private BigDecimal originalSigned = BigDecimal.ZERO;
        private BigDecimal replacementSigned = BigDecimal.ZERO;
        private String originalVersion;
        private String replacementVersion;
        private boolean customerVisible;
        private boolean affectsCustomerPayable;
        private String description;

        private void addOriginal(CalculationLine line) {
            originalSigned = originalSigned.add(line.signedAmount());
            originalVersion = line.componentVersion();
            copyDisplayFields(line);
        }

        private void addReplacement(CalculationLine line) {
            replacementSigned = replacementSigned.add(line.signedAmount());
            replacementVersion = line.componentVersion();
            copyDisplayFields(line);
        }

        private void copyDisplayFields(CalculationLine line) {
            customerVisible = customerVisible || line.customerVisible();
            affectsCustomerPayable = affectsCustomerPayable || line.affectsCustomerPayable();
            description = line.description();
        }
    }
}
