package com.titanium.product.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.RateUnit;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.RateTableRow;

/**
 * 首期费率表保费合成领域服务。
 */
@Service
public class PremiumCompositionService {

    private static final BigDecimal THOUSAND = new BigDecimal("1000");

    /**
     * 按费率单位计算保费，并应用最低/最高保费和最终两位舍入。
     */
    public BigDecimal calculate(BigDecimal sumInsured, RateUnit rateUnit, RateTableRow row) {
        if (sumInsured == null || sumInsured.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PricingDomainException(ProductErrorCode.PRICING_INPUT_INVALID, "保额必须大于 0");
        }
        if (rateUnit == null) {
            throw new PricingDomainException(ProductErrorCode.PRICING_INPUT_INVALID, "费率单位不能为空");
        }
        BigDecimal premium = switch (rateUnit) {
            case SUM_INSURED_RATIO -> sumInsured.multiply(row.rate());
            case PER_THOUSAND_SUM_INSURED -> sumInsured.multiply(row.rate()).divide(THOUSAND, 8,
                    RoundingMode.HALF_UP);
            case FIXED_AMOUNT -> row.rate();
        };
        if (row.minimumPremium() != null && premium.compareTo(row.minimumPremium()) < 0) {
            premium = row.minimumPremium();
        }
        if (row.maximumPremium() != null && premium.compareTo(row.maximumPremium()) > 0) {
            premium = row.maximumPremium();
        }
        return premium.setScale(2, RoundingMode.HALF_UP);
    }
}
