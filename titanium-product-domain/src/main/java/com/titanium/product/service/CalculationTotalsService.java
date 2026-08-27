package com.titanium.product.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.pricing.CalculationLine;
import com.titanium.product.valueobject.pricing.CalculationTotals;

/**
 * 按金额通道和费用分类汇总结构化计算明细。
 */
@Service
public class CalculationTotalsService {

    public CalculationTotals summarize(List<CalculationLine> lines) {
        BigDecimal customerPayable = BigDecimal.ZERO;
        BigDecimal taxAndLevyTotal = BigDecimal.ZERO;
        BigDecimal internalCostTotal = BigDecimal.ZERO;
        for (CalculationLine line : lines == null ? List.<CalculationLine>of() : lines) {
            BigDecimal signedAmount = line.signedAmount();
            if (line.amountChannel() == AmountChannel.INTERNAL_COST) {
                internalCostTotal = internalCostTotal.add(signedAmount);
            } else {
                if (line.affectsCustomerPayable()) {
                    customerPayable = customerPayable.add(signedAmount);
                }
                if (isTaxOrLevy(line.category())) {
                    taxAndLevyTotal = taxAndLevyTotal.add(signedAmount);
                }
            }
        }
        BigDecimal premiumSubtotal = customerPayable.subtract(taxAndLevyTotal);
        if (premiumSubtotal.signum() < 0 || taxAndLevyTotal.signum() < 0
                || customerPayable.signum() < 0 || internalCostTotal.signum() < 0) {
            throw new PricingDomainException(
                    ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED,
                    "结构化费用明细导致客户应付或内部成本小于零");
        }
        return new CalculationTotals(premiumSubtotal, taxAndLevyTotal, customerPayable, internalCostTotal);
    }

    private boolean isTaxOrLevy(ChargeCategory category) {
        return category == ChargeCategory.TAX
                || category == ChargeCategory.STAMP_DUTY
                || category == ChargeCategory.REGULATORY_LEVY;
    }
}
