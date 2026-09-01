package com.titanium.product.valueobject.pricing.premium;

import com.titanium.product.common.enums.TaxPriceMode;

/**
 * 税费明细携带的法规及版本证据。
 */
public record TaxLineEvidence(
        String jurisdictionCode,
        String regulatoryReferenceId,
        TaxPriceMode priceMode,
        String policyHash,
        boolean exempt) {

    public TaxLineEvidence {
        if (jurisdictionCode == null || jurisdictionCode.isBlank()
                || regulatoryReferenceId == null || regulatoryReferenceId.isBlank()
                || priceMode == null || policyHash == null || policyHash.length() != 64) {
            throw new IllegalArgumentException("税费法规及版本证据不完整");
        }
    }
}
