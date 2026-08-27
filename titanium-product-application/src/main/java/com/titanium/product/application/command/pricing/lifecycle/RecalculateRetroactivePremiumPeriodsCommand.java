package com.titanium.product.application.command.pricing.lifecycle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Product 追溯期间重算应用命令。 */
public record RecalculateRetroactivePremiumPeriodsCommand(
        String tenantId,
        String recalculationRequestId,
        String maintenanceId,
        String policyId,
        String analysisId,
        int analysisVersion,
        String analysisResultHash,
        String originalCalculationId,
        String replacementCalculationId,
        LocalDateTime scopeFrom,
        LocalDateTime scopeTo,
        List<AffectedPeriod> periods) {

    public RecalculateRetroactivePremiumPeriodsCommand {
        periods = periods == null ? List.of() : List.copyOf(periods);
    }

    public record AffectedPeriod(
            String periodId,
            String sourceReferenceId,
            LocalDateTime periodStart,
            BigDecimal originalAmount,
            String currency,
            String sourceEvidenceHash) {
    }
}
