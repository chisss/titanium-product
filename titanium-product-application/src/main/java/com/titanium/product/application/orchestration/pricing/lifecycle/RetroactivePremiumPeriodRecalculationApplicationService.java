package com.titanium.product.application.orchestration.pricing.lifecycle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;

import org.springframework.stereotype.Service;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.aggregate.PremiumCalculation;
import com.titanium.product.application.command.pricing.lifecycle.RecalculateRetroactivePremiumPeriodsCommand;
import com.titanium.product.application.command.pricing.lifecycle.RecalculateRetroactivePremiumPeriodsCommand.AffectedPeriod;
import com.titanium.product.application.model.RetroactivePremiumPeriodRecalculationResult;
import com.titanium.product.application.orchestration.pricing.PricingEvidenceHasher;
import com.titanium.product.common.enums.PremiumBalanceDirection;
import com.titanium.product.repository.PremiumCalculationRepository;
import com.titanium.product.service.RetroactivePremiumPeriodDifferenceService;
import com.titanium.product.valueobject.pricing.lifecycle.RetroactivePremiumPeriodDifference;

import lombok.RequiredArgsConstructor;

/** 基于不可变确认计算事实生成追溯期间价格差异。 */
@Service
@RequiredArgsConstructor
public class RetroactivePremiumPeriodRecalculationApplicationService {

    private static final String RECALCULATION_VERSION = "PERIOD_V1";

    private final PremiumCalculationRepository calculationRepository;
    private final PricingEvidenceHasher evidenceHasher;
    private final RetroactivePremiumPeriodDifferenceService differenceService =
            new RetroactivePremiumPeriodDifferenceService();

    public RetroactivePremiumPeriodRecalculationResult recalculate(
            RecalculateRetroactivePremiumPeriodsCommand command) {
        validate(command);
        PremiumCalculation original = get(command.tenantId(), command.originalCalculationId());
        PremiumCalculation replacement = get(command.tenantId(), command.replacementCalculationId());
        validateCalculations(original, replacement);
        validatePeriods(command, replacement.getCurrency());

        String inputHash = inputHash(command, original, replacement);
        List<RetroactivePremiumPeriodDifference> periods = command.periods().stream()
                .sorted(java.util.Comparator.comparing(AffectedPeriod::periodStart)
                        .thenComparing(AffectedPeriod::periodId))
                .map(period -> difference(period, replacement.getInstallmentAmount()))
                .toList();
        BigDecimal signedTotal = periods.stream()
                .map(item -> signed(item.direction(), item.differenceAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        PremiumBalanceDirection direction = direction(signedTotal);
        BigDecimal amount = signedTotal.abs();
        String resultHash = resultHash(command, original, replacement, inputHash, periods, direction, amount);
        return new RetroactivePremiumPeriodRecalculationResult(
                command.tenantId(), "rpr-" + inputHash.substring(0, 32), RECALCULATION_VERSION,
                command.recalculationRequestId(), command.maintenanceId(), command.policyId(),
                command.analysisId(), command.analysisVersion(), command.analysisResultHash().toLowerCase(Locale.ROOT),
                original.getProductId(), replacement.getEvidence().productVersion(),
                original.getCalculationId(), original.getResultHash(), replacement.getCalculationId(),
                replacement.getResultHash(), command.scopeFrom(), command.scopeTo(), direction, amount,
                replacement.getCurrency(), inputHash, resultHash, LocalDateTime.now(), periods);
    }

    private RetroactivePremiumPeriodDifference difference(
            AffectedPeriod period,
            BigDecimal replacementInstallmentAmount) {
        var difference = differenceService.compare(period.originalAmount(), replacementInstallmentAmount);
        String periodHash = evidenceHasher.hash(String.join("|", RECALCULATION_VERSION,
                period.periodId(), period.sourceReferenceId(), period.periodStart().toString(),
                amount(period.originalAmount()), amount(replacementInstallmentAmount),
                difference.direction().getCode(), amount(difference.amount()),
                period.currency().toUpperCase(Locale.ROOT), period.sourceEvidenceHash().toLowerCase(Locale.ROOT)));
        return new RetroactivePremiumPeriodDifference(
                period.periodId(), period.sourceReferenceId(), period.periodStart(), period.originalAmount(),
                replacementInstallmentAmount, difference.direction(), difference.amount(), period.currency(),
                period.sourceEvidenceHash(), periodHash);
    }

    private String inputHash(
            RecalculateRetroactivePremiumPeriodsCommand command,
            PremiumCalculation original,
            PremiumCalculation replacement) {
        StringJoiner periodJoiner = new StringJoiner(";");
        command.periods().stream()
                .sorted(java.util.Comparator.comparing(AffectedPeriod::periodStart)
                        .thenComparing(AffectedPeriod::periodId))
                .forEach(period -> periodJoiner.add(String.join(",", period.periodId(), period.sourceReferenceId(),
                        period.periodStart().toString(), amount(period.originalAmount()),
                        period.currency().toUpperCase(Locale.ROOT), period.sourceEvidenceHash().toLowerCase(Locale.ROOT))));
        return evidenceHasher.hash(String.join("|", command.tenantId(), command.recalculationRequestId(),
                command.maintenanceId(), command.policyId(), command.analysisId(),
                String.valueOf(command.analysisVersion()), command.analysisResultHash().toLowerCase(Locale.ROOT),
                original.getCalculationId(), original.getResultHash(), replacement.getCalculationId(),
                replacement.getResultHash(), command.scopeFrom().toString(), command.scopeTo().toString(),
                periodJoiner.toString()));
    }

    private String resultHash(
            RecalculateRetroactivePremiumPeriodsCommand command,
            PremiumCalculation original,
            PremiumCalculation replacement,
            String inputHash,
            List<RetroactivePremiumPeriodDifference> periods,
            PremiumBalanceDirection direction,
            BigDecimal amount) {
        StringJoiner periodJoiner = new StringJoiner(";");
        periods.forEach(period -> periodJoiner.add(period.resultHash()));
        return evidenceHasher.hash(String.join("|", RECALCULATION_VERSION, command.analysisResultHash(),
                original.getResultHash(), replacement.getResultHash(), inputHash, direction.getCode(),
                amount(amount), replacement.getCurrency(), periodJoiner.toString()));
    }

    private void validate(RecalculateRetroactivePremiumPeriodsCommand command) {
        if (command == null || blank(command.tenantId()) || blank(command.recalculationRequestId())
                || blank(command.maintenanceId()) || blank(command.policyId()) || blank(command.analysisId())
                || command.analysisVersion() < 1 || !hash(command.analysisResultHash())
                || blank(command.originalCalculationId()) || blank(command.replacementCalculationId())
                || command.scopeFrom() == null || command.scopeTo() == null
                || !command.scopeFrom().isBefore(command.scopeTo())) {
            throw invalid();
        }
    }

    private void validateCalculations(PremiumCalculation original, PremiumCalculation replacement) {
        if (!original.getProductId().equals(replacement.getProductId())
                || !original.getCurrency().equals(replacement.getCurrency())) {
            throw invalid();
        }
    }

    private void validatePeriods(
            RecalculateRetroactivePremiumPeriodsCommand command,
            String calculationCurrency) {
        Set<String> ids = new HashSet<>();
        for (AffectedPeriod period : command.periods()) {
            if (period == null || blank(period.periodId()) || !ids.add(period.periodId())
                    || blank(period.sourceReferenceId()) || period.periodStart() == null
                    || !period.periodStart().isAfter(command.scopeFrom())
                    || period.periodStart().isAfter(command.scopeTo())
                    || period.originalAmount() == null || period.originalAmount().signum() < 0
                    || blank(period.currency()) || !calculationCurrency.equalsIgnoreCase(period.currency())
                    || !hash(period.sourceEvidenceHash())) {
                throw invalid();
            }
        }
    }

    private PremiumCalculation get(String tenantId, String calculationId) {
        return calculationRepository.findById(tenantId, calculationId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRICING_CALCULATION_NOT_FOUND));
    }

    private PremiumBalanceDirection direction(BigDecimal signedAmount) {
        return signedAmount.signum() > 0 ? PremiumBalanceDirection.DEBIT
                : signedAmount.signum() < 0 ? PremiumBalanceDirection.CREDIT : PremiumBalanceDirection.NONE;
    }

    private BigDecimal signed(PremiumBalanceDirection direction, BigDecimal amount) {
        return direction == PremiumBalanceDirection.CREDIT ? amount.negate()
                : direction == PremiumBalanceDirection.DEBIT ? amount : BigDecimal.ZERO;
    }

    private String amount(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private boolean hash(String value) {
        return value != null && value.matches("[0-9a-fA-F]{64}");
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private BusinessException invalid() {
        return new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
    }
}
