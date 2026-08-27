package com.titanium.product.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.aggregate.TaxPolicyDefinition;
import com.titanium.product.common.enums.TaxPriceMode;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.pricing.CalculationLine;
import com.titanium.product.valueobject.pricing.CalculationModelExecutionResult;
import com.titanium.product.valueobject.pricing.PricingRoundingRule;
import com.titanium.product.valueobject.pricing.TaxLineEvidence;

import lombok.RequiredArgsConstructor;

/**
 * 基于已解析费用事实执行税费计算，不访问仓储或外部服务。
 */
@Service
@RequiredArgsConstructor
public class TaxCalculationService {

    private final CalculationTotalsService totalsService;

    public CalculationModelExecutionResult apply(
            List<CalculationLine> baseLines,
            List<TaxPolicyDefinition> policies,
            Map<String, Object> requestSnapshot,
            String currency,
            PricingRoundingRule roundingRule) {
        List<CalculationLine> lines = new ArrayList<>(baseLines == null ? List.of() : baseLines);
        for (TaxPolicyDefinition policy : policies == null ? List.<TaxPolicyDefinition>of() : policies) {
            lines.add(calculate(policy, lines, requestSnapshot, currency, roundingRule));
        }
        return new CalculationModelExecutionResult(
                List.copyOf(lines), totalsService.summarize(lines), "TAX_POLICY_COMPOSITION");
    }

    private CalculationLine calculate(
            TaxPolicyDefinition policy,
            List<CalculationLine> lines,
            Map<String, Object> requestSnapshot,
            String currency,
            PricingRoundingRule roundingRule) {
        Set<String> baseCodes = policy.getBaseComponentCodes().stream()
                .map(code -> code.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        List<CalculationLine> baseLines = lines.stream()
                .filter(line -> line.amountChannel() == AmountChannel.CUSTOMER_PRICE)
                .filter(line -> baseCodes.contains(line.componentCode().toUpperCase(Locale.ROOT)))
                .toList();
        if (baseLines.isEmpty()) {
            throw invalid("税费策略未匹配任何税基费用项: " + policy.getPolicyCode());
        }
        BigDecimal baseAmount = baseLines.stream()
                .map(CalculationLine::signedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (baseAmount.signum() < 0) {
            throw invalid("税基金额不能小于零: " + policy.getPolicyCode());
        }
        boolean exempt = isExempt(policy.getExemptionFeatureCode(), requestSnapshot);
        BigDecimal taxAmount = exempt
                ? BigDecimal.ZERO.setScale(roundingRule.scale(), roundingRule.roundingMode())
                : calculateAmount(policy, baseAmount, roundingRule);
        return new CalculationLine(
                "TAX-" + policy.getPolicyCode(), policy.getPolicyCode(), policy.getPolicyVersion(),
                policy.getCategory(), AmountChannel.CUSTOMER_PRICE, ChargeDirection.DEBIT,
                policy.getPayerType(), policy.getAccountingClass(), currency.toUpperCase(Locale.ROOT),
                baseAmount, policy.getTaxRate(), taxAmount, "TAX:" + policy.getPolicyCode(), true,
                exempt ? policy.getPolicyName() + "（免税）" : policy.getPolicyName(),
                policy.getPriceMode() == TaxPriceMode.EXCLUSIVE,
                new TaxLineEvidence(
                        policy.getJurisdictionCode(), policy.getRegulatoryReferenceId(), policy.getPriceMode(),
                        policy.getContentHash(), exempt));
    }

    private BigDecimal calculateAmount(
            TaxPolicyDefinition policy, BigDecimal baseAmount, PricingRoundingRule roundingRule) {
        BigDecimal amount = policy.getPriceMode() == TaxPriceMode.EXCLUSIVE
                ? baseAmount.multiply(policy.getTaxRate())
                : inclusiveTax(baseAmount, policy.getTaxRate(), roundingRule);
        return amount.setScale(roundingRule.scale(), roundingRule.roundingMode());
    }

    private BigDecimal inclusiveTax(
            BigDecimal grossAmount, BigDecimal taxRate, PricingRoundingRule roundingRule) {
        if (taxRate.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return grossAmount.multiply(taxRate).divide(
                BigDecimal.ONE.add(taxRate), roundingRule.scale() + 8, roundingRule.roundingMode());
    }

    private boolean isExempt(String featureCode, Map<String, Object> requestSnapshot) {
        if (featureCode == null) {
            return false;
        }
        Object value = requestSnapshot == null ? null : requestSnapshot.get(featureCode);
        return value instanceof Boolean booleanValue ? booleanValue
                : value != null && ("true".equalsIgnoreCase(value.toString()) || "1".equals(value.toString()));
    }

    private PricingDomainException invalid(String detail) {
        return new PricingDomainException(ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED, detail);
    }
}
