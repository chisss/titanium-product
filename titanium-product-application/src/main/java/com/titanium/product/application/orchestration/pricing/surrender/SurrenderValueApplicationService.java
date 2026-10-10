package com.titanium.product.application.orchestration.pricing.surrender;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.application.model.pricing.surrender.SurrenderValueCalculationResult;
import com.titanium.product.application.orchestration.pricing.PricingEvidenceHasher;
import com.titanium.product.application.orchestration.pricing.lifecycle.PremiumLifecycleAdjustmentApplicationService;
import com.titanium.product.command.pricing.lifecycle.CreatePremiumLifecycleAdjustmentCommand;
import com.titanium.product.command.pricing.surrender.CalculateSurrenderValueCommand;
import com.titanium.product.common.enums.PremiumLifecycleType;
import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.common.enums.SurrenderRefundBasis;
import com.titanium.product.exception.PremiumCalculationConcurrentConflictException;
import com.titanium.product.pricing.aggregate.PremiumCalculation;
import com.titanium.product.pricing.aggregate.lifecycle.PremiumLifecycleAdjustment;
import com.titanium.product.pricing.aggregate.surrender.SurrenderValuePolicy;
import com.titanium.product.repository.PremiumCalculationRepository;
import com.titanium.product.repository.SurrenderValuePolicyRepository;
import com.titanium.product.valueobject.pricing.calculation.CalculationLine;
import com.titanium.product.valueobject.pricing.calculation.CalculationTotals;
import com.titanium.product.valueobject.pricing.premium.SurrenderBalanceFacts;
import com.titanium.product.valueobject.pricing.premium.SurrenderLoanFacts;
import com.titanium.product.valueobject.pricing.premium.SurrenderPremiumFacts;
import com.titanium.product.valueobject.pricing.premium.SurrenderValueOutcome;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 退保价值确认应用编排器。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SurrenderValueApplicationService {

    private static final String REPLACEMENT_SUFFIX = ":replacement";
    private static final String ADJUSTMENT_SUFFIX = ":adjustment";

    private final PremiumCalculationRepository calculationRepository;
    private final SurrenderValuePolicyRepository surrenderValuePolicyRepository;
    private final PremiumLifecycleAdjustmentApplicationService lifecycleAdjustmentApplicationService;
    private final PricingEvidenceHasher evidenceHasher;

    public SurrenderValueCalculationResult calculate(CalculateSurrenderValueCommand command) {
        validateCommand(command);
        PremiumCalculation original = calculationRepository.findById(
                        command.tenantId(), command.originalCalculationId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRICING_CALCULATION_NOT_FOUND));
        validateOriginal(command, original);
        SurrenderValuePolicy policy = surrenderValuePolicyRepository.findPublished(
                        command.tenantId(), original.getProductId(), command.policyYear(), command.businessTime())
                .orElseThrow(() -> new BusinessException(
                        "未找到业务时点适用的已发布退保价值策略",
                        ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED));

        RoundingMode roundingMode = RoundingMode.valueOf(original.getEvidence().roundingMode());
        int scale = original.getEvidence().roundingScale();
        SurrenderPremiumFacts premiumFacts = premiumFactsOf(command);
        SurrenderLoanFacts loanFacts = loanFactsOf(command);
        SurrenderBalanceFacts balanceFacts = balanceFactsOf(command);
        SurrenderValueOutcome outcome = policy.calculate(
                original.getCalculationTotals().customerPayable(), command.policyEffectiveDate(),
                command.surrenderDate(), premiumFacts, loanFacts, balanceFacts, scale, roundingMode);
        String requestHash = requestHash(command, original, policy, premiumFacts, loanFacts, balanceFacts);
        PremiumCalculation replacement = findOrCreateReplacement(
                command, original, policy, outcome, requestHash, scale, roundingMode);
        PremiumLifecycleAdjustment adjustment = lifecycleAdjustmentApplicationService.create(
                new CreatePremiumLifecycleAdjustmentCommand(
                        command.tenantId(), command.surrenderRequestId() + ADJUSTMENT_SUFFIX,
                        command.originalBizNo(),
                        PremiumLifecycleType.SURRENDER, original.getCalculationId(), replacement.getCalculationId(),
                        command.businessTime(), command.reason()));

        if (adjustment.getCustomerAmount().compareTo(outcome.refundAmount()) != 0) {
            throw new BusinessException(
                    "退保价值与生命周期客户贷项无法勾稽",
                    ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED);
        }
        log.info("Product退保价值已确认: requestId={}, adjustmentId={}, refundType={}, amount={}",
                command.surrenderRequestId(), adjustment.getAdjustmentId(), outcome.refundType(), outcome.refundAmount());
        return toResult(command, policy, outcome, replacement, adjustment);
    }

    private PremiumCalculation findOrCreateReplacement(
            CalculateSurrenderValueCommand command,
            PremiumCalculation original,
            SurrenderValuePolicy policy,
            SurrenderValueOutcome outcome,
            String requestHash,
            int scale,
            RoundingMode roundingMode) {
        String calculationRequestId = command.surrenderRequestId() + REPLACEMENT_SUFFIX;
        Optional<PremiumCalculation> existing = calculationRepository.findByIdempotencyKey(
                command.tenantId(), calculationRequestId, PricingCalculationPurpose.MAINTENANCE);
        if (existing.isPresent()) {
            existing.get().assertSameRequest(requestHash);
            return existing.get();
        }

        BigDecimal customerRetentionRate = BigDecimal.ONE.subtract(outcome.cashValueRate());
        CalculationTotals totals = replacementTotals(
                original.getCalculationTotals(), outcome.retainedCustomerAmount(), customerRetentionRate,
                outcome.internalCostRetentionRate(), scale, roundingMode);
        List<CalculationLine> lines = replacementLines(
                original.getCalculationLines(), totals.customerPayable(), customerRetentionRate,
                outcome.internalCostRetentionRate(), scale, roundingMode);
        Map<String, Object> snapshot = surrenderSnapshot(command, policy, outcome);
        String inputHash = evidenceHasher.hash(String.join("|", original.getResultHash(), policy.getContentHash(),
                command.policyEffectiveDate().toString(), command.surrenderDate().toString(),
                Integer.toString(command.policyYear())));
        String resultHash = resultHash(original, policy, outcome, totals, lines);
        PremiumCalculation replacement = PremiumCalculation.confirm(
                UUID.randomUUID().toString(), calculationRequestId, command.originalBizNo(),
                PricingCalculationPurpose.MAINTENANCE, command.tenantId(), original.getProductId(),
                command.businessTime(), original.getCurrency(), totals.premiumSubtotal(), totals.customerPayable(),
                totals.customerPayable().divide(BigDecimal.valueOf(original.getPeriods()), scale, roundingMode),
                original.getPeriods(), List.of(), totals, lines, original.getEvidence(), snapshot,
                requestHash, inputHash, resultHash, LocalDateTime.now());
        try {
            calculationRepository.save(replacement);
            return replacement;
        } catch (PremiumCalculationConcurrentConflictException exception) {
            return calculationRepository.findByIdempotencyKey(
                            command.tenantId(), calculationRequestId, PricingCalculationPurpose.MAINTENANCE)
                    .map(winner -> {
                        winner.assertSameRequest(requestHash);
                        return winner;
                    })
                    .orElseThrow(() -> exception);
        }
    }

    private CalculationTotals replacementTotals(
            CalculationTotals original,
            BigDecimal retainedCustomerAmount,
            BigDecimal customerRetentionRate,
            BigDecimal internalRetentionRate,
            int scale,
            RoundingMode roundingMode) {
        BigDecimal retainedTax = scaled(original.taxAndLevyTotal(), customerRetentionRate, scale, roundingMode)
                .min(retainedCustomerAmount);
        BigDecimal retainedPremium = retainedCustomerAmount.subtract(retainedTax);
        BigDecimal retainedInternal = scaled(
                original.internalCostTotal(), internalRetentionRate, scale, roundingMode);
        return new CalculationTotals(retainedPremium, retainedTax, retainedCustomerAmount, retainedInternal);
    }

    private List<CalculationLine> replacementLines(
            List<CalculationLine> originals,
            BigDecimal targetCustomerAmount,
            BigDecimal customerRetentionRate,
            BigDecimal internalRetentionRate,
            int scale,
            RoundingMode roundingMode) {
        List<CalculationLine> lines = new ArrayList<>();
        for (CalculationLine original : originals) {
            BigDecimal factor = original.amountChannel() == AmountChannel.CUSTOMER_PRICE
                    ? customerRetentionRate
                    : internalRetentionRate;
            lines.add(copyWithAmount(original, scaled(original.calculatedAmount(), factor, scale, roundingMode)));
        }
        rebalanceCustomerLines(lines, targetCustomerAmount);
        return List.copyOf(lines);
    }

    private void rebalanceCustomerLines(List<CalculationLine> lines, BigDecimal target) {
        BigDecimal actual = lines.stream()
                .filter(CalculationLine::affectsCustomerPayable)
                .map(CalculationLine::signedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainder = target.subtract(actual);
        if (remainder.signum() == 0) {
            return;
        }
        for (int index = lines.size() - 1; index >= 0; index--) {
            CalculationLine line = lines.get(index);
            if (!line.affectsCustomerPayable()) {
                continue;
            }
            BigDecimal adjusted = line.direction() == ChargeDirection.DEBIT
                    ? line.calculatedAmount().add(remainder)
                    : line.calculatedAmount().subtract(remainder);
            if (adjusted.signum() >= 0) {
                lines.set(index, copyWithAmount(line, adjusted));
                return;
            }
        }
        throw new BusinessException(
                "退保后客户费用明细无法分摊尾差",
                ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED);
    }

    private CalculationLine copyWithAmount(CalculationLine source, BigDecimal amount) {
        return new CalculationLine(
                source.lineId(), source.componentCode(), source.componentVersion(), source.category(),
                source.amountChannel(), source.direction(), source.payerType(), source.accountingClass(),
                source.currency(), source.baseAmount(), source.rate(), amount, source.nodeCode(),
                source.customerVisible(), source.description(), source.affectsCustomerPayable(),
                source.taxEvidence(), source.commissionEvidence());
    }

    private Map<String, Object> surrenderSnapshot(
            CalculateSurrenderValueCommand command,
            SurrenderValuePolicy policy,
            SurrenderValueOutcome outcome) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("surrenderPolicyCode", policy.getPolicyCode());
        snapshot.put("surrenderPolicyVersion", policy.getPolicyVersion());
        snapshot.put("surrenderPolicyHash", policy.getContentHash());
        snapshot.put("policyYear", command.policyYear());
        snapshot.put("coolingOffDays", policy.getCoolingOffDays());
        snapshot.put("refundBasis", policy.getRefundBasis().getCode());
        snapshot.put("policyEffectiveDate", command.policyEffectiveDate().toString());
        snapshot.put("surrenderDate", command.surrenderDate().toString());
        snapshot.put("refundType", outcome.refundType().name());
        snapshot.put("cashValueRate", outcome.cashValueRate());
        snapshot.put("refundAmount", outcome.refundAmount());
        snapshot.put("internalCostRetentionRate", outcome.internalCostRetentionRate());
        return snapshot;
    }

    /**
     * 命令中的保费事实 → 领域值对象（保单域下发，本层只做搬运，不推导任何保费口径）。
     */
    private SurrenderPremiumFacts premiumFactsOf(CalculateSurrenderValueCommand command) {
        return new SurrenderPremiumFacts(
                command.annualPremium(), command.coverageEndDate(), command.premiumPaidUp());
    }

    /**
     * 命令中的贷款未偿本息 → 领域值对象（保单域按 g04-03 同一贷款口径下发，本层只做搬运）。
     * <p>
     * 🔴 取不到（两项为 null）时**照原样**传给策略，由策略在使用点失败关闭——本层不做任何
     * 「无贷款则置 0」的兜底，否则取数失败会被伪装成「无未结贷款」（AC-05）。
     * </p>
     */
    private SurrenderLoanFacts loanFactsOf(CalculateSurrenderValueCommand command) {
        return new SurrenderLoanFacts(
                command.loanOutstandingPrincipal(), command.loanOutstandingInterest());
    }

    /**
     * 命令中的账户余额事实 → 领域值对象（保单域财务快照下发，本层只做搬运）。
     * <p>
     * 🔴 取不到（三项为 null）时**照原样**传给策略，由策略在使用点失败关闭——本层不做任何
     * 「无余额则置 0」的兜底，否则取数失败会被伪装成「无欠费、无红利、无账户价值」（AC-05）。
     * </p>
     * <p>
     * 🔴 三项**不得在本层先行合并**：「红利 + 投资账户价值 = 红利及账户价值」是一个退费因子的
     * 合并口径，属产品域（退费口径权威域）的决策；本层与保全域适配器只搬运原始分量。
     * </p>
     */
    private SurrenderBalanceFacts balanceFactsOf(CalculateSurrenderValueCommand command) {
        return new SurrenderBalanceFacts(
                command.overduePremium(), command.dividendBalance(), command.accountValue());
    }

    /**
     * 请求哈希：相同 surrenderRequestId 必须对应同一组影响结果的入参。
     *
     * @param loanFacts    贷款事实；调用点保证其**必可得**——策略在 {@code calculate} 中已按 AC-05
     *                     失败关闭，能走到本方法即说明未偿本息已取到
     * @param balanceFacts 账户余额事实；同上，调用点保证其**必可得**
     */
    private String requestHash(
            CalculateSurrenderValueCommand command,
            PremiumCalculation original,
            SurrenderValuePolicy policy,
            SurrenderPremiumFacts premiumFacts,
            SurrenderLoanFacts loanFacts,
            SurrenderBalanceFacts balanceFacts) {
        String base = String.join("|", command.tenantId(), command.surrenderRequestId(),
                command.bizNo(), command.originalBizNo(), original.getCalculationId(), original.getResultHash(),
                policy.getPolicyCode(),
                policy.getPolicyVersion(), policy.getContentHash(), command.policyEffectiveDate().toString(),
                command.surrenderDate().toString(), Integer.toString(command.policyYear()),
                command.businessTime().toString(), command.reason());
        // 🔴 保费事实只在「未满期保费基准」下影响结果，故仅在此时纳入哈希：现金价值基准（存量全部
        // 策略行）的请求哈希与改造前**逐字相同**，升级后重放同一 surrenderRequestId 不会因哈希
        // 变化被判幂等冲突。若一律纳入，存量在途退保请求在服务升级后会莫名其妙地失败。
        String withPremiumFacts = policy.getRefundBasis() != SurrenderRefundBasis.UNEARNED_PREMIUM
                ? base
                : String.join("|", base,
                        premiumFacts.premiumPaidUp() ? "PAID_UP" : "IN_PAYMENT",
                        premiumFacts.annualPremium() == null ? "*" : premiumFacts.annualPremium().toPlainString(),
                        premiumFacts.coverageEndDate() == null ? "*" : premiumFacts.coverageEndDate().toString());
        // 🔴 贷款事实只在**非零**时影响结果（无未结贷款下贷款因子恒为 DEBIT/0，净额与改造前逐字
        // 相同），故仅在非零时纳入哈希：存量无贷款的在途退保请求升级后不会被判幂等冲突。有非零
        // 贷款的请求若被重放，则明确判为幂等冲突（其应退金额确实已变），而不是静默复用旧结果。
        String withLoanFacts = loanFacts.outstandingPrincipal().signum() == 0
                && loanFacts.outstandingInterest().signum() == 0
                ? withPremiumFacts
                : String.join("|", withPremiumFacts,
                        loanFacts.outstandingPrincipal().stripTrailingZeros().toPlainString(),
                        loanFacts.outstandingInterest().stripTrailingZeros().toPlainString());
        // 🔴 账户余额事实同规则：三项**全 0** 时欠缴因子恒为 DEBIT/0、权益因子恒为 CREDIT/0，净额与
        // 改造前逐字相同，故不纳入哈希；任一非零才纳入（其应退金额确实已变，重放应判幂等冲突）。
        if (balanceFacts.overduePremium().signum() == 0
                && balanceFacts.dividendBalance().signum() == 0
                && balanceFacts.accountValue().signum() == 0) {
            return evidenceHasher.hash(withLoanFacts);
        }
        return evidenceHasher.hash(String.join("|", withLoanFacts,
                balanceFacts.overduePremium().stripTrailingZeros().toPlainString(),
                balanceFacts.dividendBalance().stripTrailingZeros().toPlainString(),
                balanceFacts.accountValue().stripTrailingZeros().toPlainString()));
    }

    private String resultHash(
            PremiumCalculation original,
            SurrenderValuePolicy policy,
            SurrenderValueOutcome outcome,
            CalculationTotals totals,
            List<CalculationLine> lines) {
        StringJoiner lineEvidence = new StringJoiner(";");
        lines.forEach(line -> lineEvidence.add(String.join(",", line.lineId(), line.componentCode(),
                line.direction().getCode(), line.calculatedAmount().stripTrailingZeros().toPlainString())));
        return evidenceHasher.hash(String.join("|", original.getResultHash(), policy.getContentHash(),
                outcome.refundType().name(), outcome.cashValueRate().stripTrailingZeros().toPlainString(),
                outcome.refundAmount().stripTrailingZeros().toPlainString(),
                totals.customerPayable().stripTrailingZeros().toPlainString(),
                totals.internalCostTotal().stripTrailingZeros().toPlainString(), lineEvidence.toString()));
    }

    private SurrenderValueCalculationResult toResult(
            CalculateSurrenderValueCommand command,
            SurrenderValuePolicy policy,
            SurrenderValueOutcome outcome,
            PremiumCalculation replacement,
            PremiumLifecycleAdjustment adjustment) {
        return new SurrenderValueCalculationResult(
                command.surrenderRequestId(), policy.getPolicyCode(), policy.getPolicyVersion(),
                policy.getContentHash(), command.policyYear(), policy.getCoolingOffDays(),
                policy.getRefundBasis(), outcome.refundType(),
                outcome.withinCoolingOff(), outcome.cashValueRate(), outcome.refundAmount(),
                outcome.retainedCustomerAmount(), outcome.internalCostRetentionRate(), outcome.breakdown().factors(),
                adjustment.getRequestHash(), adjustment.getOriginalResultHash(),
                adjustment.getReplacementResultHash(), replacement.getEvidence().pricingPlanVersion(),
                replacement.getEvidence().pricingPlanContentHash(), adjustment);
    }

    private BigDecimal scaled(BigDecimal amount, BigDecimal factor, int scale, RoundingMode roundingMode) {
        return amount.multiply(factor).setScale(scale, roundingMode);
    }

    private void validateCommand(CalculateSurrenderValueCommand command) {
        if (command == null || blank(command.tenantId()) || blank(command.surrenderRequestId())
                || blank(command.bizNo()) || blank(command.originalBizNo()) || blank(command.originalCalculationId())
                || command.policyEffectiveDate() == null || command.surrenderDate() == null
                || command.surrenderDate().isBefore(command.policyEffectiveDate())
                || command.policyYear() == null || command.policyYear() < 1 || command.businessTime() == null
                || blank(command.reason())) {
            throw new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
        }
    }

    private void validateOriginal(CalculateSurrenderValueCommand command, PremiumCalculation original) {
        if (original.getPurpose() != PricingCalculationPurpose.ISSUANCE_CONFIRM
                || !Objects.equals(original.getBizNo(), command.originalBizNo())) {
            throw new BusinessException(
                    "退保价值必须基于同一保单的出单确认计算",
                    ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED);
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
