package com.titanium.product.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.PremiumAdjustmentType;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.pricing.calculation.CalculationLine;
import com.titanium.product.valueobject.pricing.calculation.CalculationModelExecutionResult;
import com.titanium.product.valueobject.pricing.calculation.CalculationTotals;
import com.titanium.product.valueobject.pricing.premium.PremiumAdjustment;

import lombok.RequiredArgsConstructor;

/**
 * 将定价结果与核保调整合成为最终结构化费用明细。
 */
@Service
@RequiredArgsConstructor
public class PremiumCalculationBreakdownService {

    private final CalculationTotalsService totalsService;

    public CalculationModelExecutionResult applyAdjustments(
            List<CalculationLine> baseLines,
            BigDecimal baseCustomerPayable,
            List<PremiumAdjustment> adjustments,
            String currency) {
        List<CalculationLine> lines = new ArrayList<>(baseLines == null ? List.of() : baseLines);
        if (lines.isEmpty()) {
            lines.add(legacyBaseLine(baseCustomerPayable, currency));
        }
        CalculationTotals baseTotals = totalsService.summarize(lines);
        if (baseCustomerPayable == null
                || baseTotals.customerPayable().compareTo(baseCustomerPayable) != 0) {
            throw invalid("基础费用明细与试算客户应付不一致");
        }
        List<PremiumAdjustment> safeAdjustments = adjustments == null ? List.of() : adjustments;
        for (int index = 0; index < safeAdjustments.size(); index++) {
            lines.add(toLine(safeAdjustments.get(index), index + 1, currency));
        }
        return new CalculationModelExecutionResult(
                lines, totalsService.summarize(lines), "FINAL_CUSTOMER_PAYABLE");
    }

    private CalculationLine legacyBaseLine(BigDecimal amount, String currency) {
        return new CalculationLine(
                "LEGACY_BASE_PREMIUM", "LEGACY_BASE_PREMIUM", "V1",
                ChargeCategory.RISK_PREMIUM, AmountChannel.CUSTOMER_PRICE, ChargeDirection.DEBIT,
                ChargePayerType.POLICYHOLDER, "PREMIUM", normalizeCurrency(currency), amount,
                null, amount, "LEGACY_BASE_PREMIUM", true, "兼容基础保费");
    }

    private CalculationLine toLine(PremiumAdjustment adjustment, int sequence, String currency) {
        BigDecimal signedAmount = adjustment.adjustmentAmount();
        ChargeDirection direction = signedAmount.signum() < 0
                ? ChargeDirection.CREDIT
                : ChargeDirection.DEBIT;
        BigDecimal baseAmount = adjustment.premiumAfter().subtract(signedAmount);
        BigDecimal rate = isRate(adjustment.type()) ? adjustment.value() : null;
        String version = adjustment.ruleVersion() == null || adjustment.ruleVersion().isBlank()
                ? "REQUEST"
                : adjustment.ruleVersion();
        String lineId = "UW_ADJUSTMENT_%03d".formatted(sequence);
        return new CalculationLine(
                lineId, adjustment.adjustmentCode(), version, ChargeCategory.UNDERWRITING_ADJUSTMENT,
                AmountChannel.CUSTOMER_PRICE, direction, ChargePayerType.POLICYHOLDER,
                "PREMIUM_ADJUSTMENT", normalizeCurrency(currency), baseAmount, rate, signedAmount.abs(),
                lineId, true, adjustment.reason());
    }

    private boolean isRate(PremiumAdjustmentType type) {
        return type == PremiumAdjustmentType.SURCHARGE_RATE || type == PremiumAdjustmentType.DISCOUNT_RATE;
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.length() != 3) {
            throw invalid("费用明细币种不合法");
        }
        return currency.toUpperCase(Locale.ROOT);
    }

    private PricingDomainException invalid(String detail) {
        return new PricingDomainException(ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED, detail);
    }
}
