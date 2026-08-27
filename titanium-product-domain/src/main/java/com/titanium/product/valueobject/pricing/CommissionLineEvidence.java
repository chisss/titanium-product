package com.titanium.product.valueobject.pricing;

import java.math.BigDecimal;

/**
 * 佣金费用行携带的 Channel 方案与结算证据。
 */
public record CommissionLineEvidence(
        String channelId,
        String schemeCode,
        String schemeVersion,
        String schemeHash,
        String beneficiaryType,
        String beneficiaryId,
        BigDecimal splitRate,
        BigDecimal grossCommission,
        int installmentCount,
        int clawbackMonths) {

    public CommissionLineEvidence {
        if (channelId == null || channelId.isBlank() || schemeCode == null || schemeCode.isBlank()
                || schemeVersion == null || schemeVersion.isBlank()
                || schemeHash == null || schemeHash.length() != 64
                || beneficiaryType == null || beneficiaryType.isBlank()
                || beneficiaryId == null || beneficiaryId.isBlank()
                || splitRate == null || splitRate.signum() <= 0 || grossCommission == null
                || grossCommission.signum() < 0 || installmentCount < 1 || clawbackMonths < 0) {
            throw new IllegalArgumentException("佣金方案与受益方证据不完整");
        }
    }
}
