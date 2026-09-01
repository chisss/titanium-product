package com.titanium.product.application.orchestration.pricing.lifecycle;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.application.orchestration.pricing.PricingEvidenceHasher;
import com.titanium.product.command.pricing.lifecycle.CreatePremiumLifecycleAdjustmentCommand;
import com.titanium.product.command.pricing.lifecycle.CreatePremiumLifecycleReversalCommand;
import com.titanium.product.common.enums.PremiumLifecycleType;
import com.titanium.product.exception.PremiumLifecycleAdjustmentConcurrentConflictException;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.pricing.aggregate.PremiumCalculation;
import com.titanium.product.pricing.aggregate.lifecycle.PremiumLifecycleAdjustment;
import com.titanium.product.repository.PremiumCalculationRepository;
import com.titanium.product.repository.PremiumLifecycleAdjustmentRepository;
import com.titanium.product.service.PremiumLifecycleDifferenceService;
import com.titanium.product.valueobject.pricing.premium.PremiumLifecycleDifference;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 生命周期费用差额事实应用编排器。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PremiumLifecycleAdjustmentApplicationService {

    private final PremiumCalculationRepository calculationRepository;
    private final PremiumLifecycleAdjustmentRepository adjustmentRepository;
    private final PricingEvidenceHasher evidenceHasher;
    private final PremiumLifecycleDifferenceService differenceService = new PremiumLifecycleDifferenceService();

    public PremiumLifecycleAdjustment create(CreatePremiumLifecycleAdjustmentCommand command) {
        PremiumCalculation original = getCalculation(command.tenantId(), command.originalCalculationId());
        PremiumCalculation replacement = getCalculation(command.tenantId(), command.replacementCalculationId());
        String requestHash = requestHash(command, original, replacement);
        Optional<PremiumLifecycleAdjustment> existing = adjustmentRepository.findByRequestId(
                command.tenantId(), command.adjustmentRequestId());
        if (existing.isPresent()) {
            existing.get().assertSameRequest(requestHash);
            return existing.get();
        }

        PremiumLifecycleDifference difference = differenceService.compare(original, replacement);
        String resultHash = resultHash(command, original, replacement, difference);
        PremiumLifecycleAdjustment adjustment = PremiumLifecycleAdjustment.confirm(
                UUID.randomUUID().toString(), command.adjustmentRequestId(), command.bizNo(),
                command.lifecycleType(), command.tenantId(), original.getProductId(),
                original.getCalculationId(), original.getResultHash(), replacement.getCalculationId(),
                replacement.getResultHash(), command.businessTime(), original.getCurrency(), difference,
                command.reason(), requestHash, resultHash, persistenceTime());
        try {
            adjustmentRepository.save(adjustment);
            log.info("Product生命周期费用差额已确认: adjustmentId={}, type={}, direction={}, amount={}",
                    adjustment.getAdjustmentId(), adjustment.getLifecycleType(), adjustment.getDirection(),
                    adjustment.getCustomerAmount());
            return adjustment;
        } catch (PremiumLifecycleAdjustmentConcurrentConflictException exception) {
            return adjustmentRepository.findByRequestId(command.tenantId(), command.adjustmentRequestId())
                    .map(winner -> {
                        winner.assertSameRequest(requestHash);
                        return winner;
                    })
                    .orElseThrow(() -> exception);
        }
    }

    public PremiumLifecycleAdjustment get(String tenantId, String adjustmentId) {
        return adjustmentRepository.findById(tenantId, adjustmentId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRICING_CALCULATION_NOT_FOUND));
    }

    public PremiumLifecycleAdjustment createReversal(CreatePremiumLifecycleReversalCommand command) {
        if (command == null || blank(command.tenantId()) || blank(command.adjustmentRequestId())
                || blank(command.sourceAdjustmentId()) || command.businessTime() == null
                || blank(command.reason())) {
            throw new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
        }
        PremiumLifecycleAdjustment source = get(command.tenantId(), command.sourceAdjustmentId());
        if (source.getLifecycleType() == PremiumLifecycleType.REVERSAL) {
            throw new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
        }
        String requestHash = evidenceHasher.hash(String.join("|", command.tenantId(),
                command.adjustmentRequestId(), command.sourceAdjustmentId(), command.businessTime().toString(),
                command.reason(), source.getResultHash()));
        Optional<PremiumLifecycleAdjustment> existing = adjustmentRepository.findByRequestId(
                command.tenantId(), command.adjustmentRequestId());
        if (existing.isPresent()) {
            existing.get().assertSameRequest(requestHash);
            return existing.get();
        }
        adjustmentRepository.findByReversalOfAdjustmentId(command.tenantId(), command.sourceAdjustmentId())
                .ifPresent(previous -> {
                    throw new PricingDomainException(
                            ProductErrorCode.PRICING_IDEMPOTENCY_CONFLICT,
                            "原差额事实已被冲正: " + command.sourceAdjustmentId());
                });

        PremiumLifecycleDifference difference = new PremiumLifecycleDifference(
                source.getDirection(), source.getCustomerAmount(), source.getTaxDirection(), source.getTaxAmount(),
                source.getInternalCostDirection(), source.getInternalCostAmount(), source.getLines()).reverse();
        String resultHash = evidenceHasher.hash(String.join("|", "REVERSAL", source.getAdjustmentId(),
                source.getResultHash(), difference.direction().getCode(),
                difference.customerAmount().stripTrailingZeros().toPlainString()));
        PremiumLifecycleAdjustment reversal = PremiumLifecycleAdjustment.confirmReversal(
                UUID.randomUUID().toString(), command.adjustmentRequestId(), source.getAdjustmentId(),
                source.getBizNo(), PremiumLifecycleType.REVERSAL, command.tenantId(), source.getProductId(),
                source.getReplacementCalculationId(), source.getReplacementResultHash(),
                source.getOriginalCalculationId(), source.getOriginalResultHash(), command.businessTime(),
                source.getCurrency(), difference, command.reason(), requestHash, resultHash, persistenceTime());
        try {
            adjustmentRepository.save(reversal);
            return reversal;
        } catch (PremiumLifecycleAdjustmentConcurrentConflictException exception) {
            return adjustmentRepository.findByRequestId(command.tenantId(), command.adjustmentRequestId())
                    .map(winner -> {
                        winner.assertSameRequest(requestHash);
                        return winner;
                    })
                    .orElseThrow(() -> exception);
        }
    }

    private PremiumCalculation getCalculation(String tenantId, String calculationId) {
        return calculationRepository.findById(tenantId, calculationId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRICING_CALCULATION_NOT_FOUND));
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private LocalDateTime persistenceTime() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private String requestHash(
            CreatePremiumLifecycleAdjustmentCommand command,
            PremiumCalculation original,
            PremiumCalculation replacement) {
        return evidenceHasher.hash(String.join("|", command.tenantId(), command.adjustmentRequestId(),
                command.bizNo(), command.lifecycleType().getCode(), original.getCalculationId(),
                original.getResultHash(), replacement.getCalculationId(), replacement.getResultHash(),
                command.businessTime().toString(), command.reason()));
    }

    private String resultHash(
            CreatePremiumLifecycleAdjustmentCommand command,
            PremiumCalculation original,
            PremiumCalculation replacement,
            PremiumLifecycleDifference difference) {
        StringJoiner lines = new StringJoiner(";");
        difference.lines().forEach(line -> lines.add(String.join(",", line.lineId(), line.componentCode(),
                line.category().getCode(), line.amountChannel().getCode(), line.direction().getCode(),
                line.differenceAmount().stripTrailingZeros().toPlainString())));
        return evidenceHasher.hash(String.join("|", command.lifecycleType().getCode(),
                original.getResultHash(), replacement.getResultHash(), difference.direction().getCode(),
                difference.customerAmount().stripTrailingZeros().toPlainString(),
                difference.taxDirection().getCode(), difference.taxAmount().stripTrailingZeros().toPlainString(),
                difference.internalCostDirection().getCode(),
                difference.internalCostAmount().stripTrailingZeros().toPlainString(), lines.toString()));
    }
}
