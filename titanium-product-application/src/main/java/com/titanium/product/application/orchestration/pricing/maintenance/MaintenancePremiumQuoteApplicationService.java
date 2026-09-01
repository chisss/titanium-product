package com.titanium.product.application.orchestration.pricing.maintenance;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.application.model.pricing.MaintenancePremiumQuoteResult;
import com.titanium.product.application.orchestration.pricing.PremiumCalculationApplicationService;
import com.titanium.product.application.orchestration.pricing.PricingEvidenceHasher;
import com.titanium.product.application.orchestration.pricing.lifecycle.PremiumLifecycleAdjustmentApplicationService;
import com.titanium.product.command.maintenance.CreateMaintenancePremiumQuoteCommand;
import com.titanium.product.command.maintenance.CreateMaintenancePremiumQuoteCommand.SnapshotReference;
import com.titanium.product.command.pricing.PremiumCalculationCommand;
import com.titanium.product.command.pricing.lifecycle.CreatePremiumLifecycleAdjustmentCommand;
import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.pricing.aggregate.PremiumCalculation;
import com.titanium.product.pricing.aggregate.lifecycle.PremiumLifecycleAdjustment;
import com.titanium.product.repository.PremiumCalculationRepository;
import com.titanium.product.valueobject.pricing.premium.PremiumAdjustmentRequest;

import lombok.RequiredArgsConstructor;

/** 复用确认计算与生命周期差额事实，编排 Product 保全专用报价。 */
@Service
@RequiredArgsConstructor
public class MaintenancePremiumQuoteApplicationService {

    private static final long QUOTE_VALID_HOURS = 24L;

    private final PremiumCalculationApplicationService calculationApplicationService;
    private final PremiumLifecycleAdjustmentApplicationService lifecycleApplicationService;
    private final PremiumCalculationRepository calculationRepository;
    private final PricingEvidenceHasher evidenceHasher;

    public MaintenancePremiumQuoteResult quote(CreateMaintenancePremiumQuoteCommand command) {
        validate(command);
        String payloadHash = payloadHash(command);
        if (!payloadHash.equalsIgnoreCase(command.payloadHash())) {
            throw new BusinessException("保全报价载荷摘要校验失败", ProductErrorCode.PRICING_INPUT_INVALID);
        }

        PremiumCalculation original = calculationRepository
                .findById(command.tenantId(), command.originalCalculationId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRICING_CALCULATION_NOT_FOUND));
        validateOriginal(command, original);

        PremiumCalculation replacement = calculationApplicationService.confirm(
                new PremiumCalculationCommand(
                        command.tenantId(), command.productId(), command.idempotencyKey(),
                        original.getBizNo(), PricingCalculationPurpose.MAINTENANCE,
                        command.productVersion(), command.planVersion(), command.businessTime(),
                        command.currency(), command.sumInsured(), command.age(), command.gender(),
                        command.paymentTermYears(), command.coverageTermYears(), command.paymentPeriods(),
                        requestSnapshot(command), command.underwritingAdjustments(), command.channelId(),
                        command.policyYear()));
        PremiumLifecycleAdjustment adjustment = lifecycleApplicationService.create(
                new CreatePremiumLifecycleAdjustmentCommand(
                        command.tenantId(), command.idempotencyKey(), command.maintenanceId(),
                        command.lifecycleType(), command.originalCalculationId(), replacement.getCalculationId(),
                        command.businessTime(), command.reason()));
        return result(command, payloadHash, replacement, adjustment);
    }

    /** Maintenance 与 Product 共同遵守的完整报价载荷摘要。 */
    public String payloadHash(CreateMaintenancePremiumQuoteCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", command.tenantId());
        payload.put("productId", command.productId());
        payload.put("maintenanceId", command.maintenanceId());
        payload.put("policyId", command.policyId());
        payload.put("policyBaselineVersion", command.policyBaselineVersion());
        payload.put("itemCode", command.itemCode());
        payload.put("productVersion", command.productVersion());
        payload.put("planVersion", command.planVersion());
        payload.put("lifecycleType", command.lifecycleType() == null ? null : command.lifecycleType().getCode());
        payload.put("beforeSnapshot", snapshot(command.beforeSnapshot()));
        payload.put("proposedSnapshot", snapshot(command.proposedSnapshot()));
        payload.put("originalCalculationId", command.originalCalculationId());
        payload.put("businessTime", command.businessTime());
        payload.put("currency", upper(command.currency()));
        payload.put("sumInsured", command.sumInsured());
        payload.put("age", command.age());
        payload.put("gender", command.gender());
        payload.put("paymentTermYears", command.paymentTermYears());
        payload.put("coverageTermYears", command.coverageTermYears());
        payload.put("paymentPeriods", command.paymentPeriods());
        payload.put("pricingFactors", command.pricingFactors());
        payload.put("underwritingAdjustments", adjustments(command.underwritingAdjustments()));
        payload.put("channelId", command.channelId());
        payload.put("policyYear", command.policyYear());
        payload.put("reason", command.reason());
        payload.put("idempotencyKey", command.idempotencyKey());
        return evidenceHasher.hash(evidenceHasher.canonicalValue(payload));
    }

    private Map<String, Object> requestSnapshot(CreateMaintenancePremiumQuoteCommand command) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("maintenanceId", command.maintenanceId());
        snapshot.put("policyId", command.policyId());
        snapshot.put("policyBaselineVersion", command.policyBaselineVersion());
        snapshot.put("itemCode", command.itemCode());
        snapshot.put("productVersion", command.productVersion());
        snapshot.put("planVersion", command.planVersion());
        snapshot.put("lifecycleType", command.lifecycleType().getCode());
        snapshot.put("beforeSnapshot", snapshot(command.beforeSnapshot()));
        snapshot.put("proposedSnapshot", snapshot(command.proposedSnapshot()));
        snapshot.put("pricingFactors", command.pricingFactors());
        return snapshot;
    }

    private Map<String, Object> snapshot(SnapshotReference reference) {
        if (reference == null) {
            return Map.of();
        }
        return Map.of(
                "storageKey", reference.storageKey(),
                "contentHash", reference.contentHash(),
                "policyVersion", reference.policyVersion(),
                "capturedAt", reference.capturedAt().toString());
    }

    private List<Map<String, Object>> adjustments(List<PremiumAdjustmentRequest> requests) {
        return requests.stream().map(request -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("adjustmentCode", request.adjustmentCode());
            value.put("type", request.type() == null ? null : request.type().getCode());
            value.put("value", request.value());
            value.put("reason", request.reason());
            value.put("ruleVersion", request.ruleVersion());
            return value;
        }).toList();
    }

    private MaintenancePremiumQuoteResult result(
            CreateMaintenancePremiumQuoteCommand command,
            String payloadHash,
            PremiumCalculation replacement,
            PremiumLifecycleAdjustment adjustment) {
        String detailSummary = String.format(
                Locale.ROOT, "%s %s %s; lines=%d", adjustment.getDirection().getCode(),
                adjustment.getCustomerAmount().stripTrailingZeros().toPlainString(),
                adjustment.getCurrency(), adjustment.getLines().size());
        return new MaintenancePremiumQuoteResult(
                command.tenantId(), command.maintenanceId(), command.policyId(),
                command.policyBaselineVersion(), command.productId(), command.productVersion(),
                command.planVersion(), command.itemCode(), command.beforeSnapshot().contentHash(),
                command.proposedSnapshot().contentHash(), adjustment.getAdjustmentId(),
                adjustment.getResultHash(), adjustment.getOriginalCalculationId(),
                adjustment.getOriginalResultHash(), adjustment.getReplacementCalculationId(),
                adjustment.getReplacementResultHash(), replacement.getEvidence().pricingPlanVersion(),
                replacement.getEvidence().pricingPlanContentHash(), command.idempotencyKey(), payloadHash,
                adjustment.getResultHash(), detailSummary, adjustment.getDirection().getCode(),
                adjustment.getCustomerAmount(), adjustment.getCurrency(), adjustment.getCreatedAt(),
                adjustment.getCreatedAt().plusHours(QUOTE_VALID_HOURS));
    }

    private void validate(CreateMaintenancePremiumQuoteCommand command) {
        if (command == null || blank(command.tenantId()) || blank(command.productId())
                || blank(command.maintenanceId()) || blank(command.policyId())
                || command.policyBaselineVersion() < 0 || blank(command.itemCode())
                || blank(command.productVersion()) || blank(command.planVersion())
                || command.lifecycleType() == null || command.beforeSnapshot() == null
                || command.proposedSnapshot() == null || blank(command.originalCalculationId())
                || command.businessTime() == null || blank(command.currency())
                || command.sumInsured() == null || command.sumInsured().signum() <= 0
                || command.age() < 0 || command.age() > 120 || blank(command.gender())
                || command.paymentTermYears() <= 0 || command.coverageTermYears() <= 0
                || command.paymentPeriods() <= 0 || command.policyYear() <= 0
                || blank(command.reason()) || blank(command.idempotencyKey())
                || !hash(command.payloadHash()) || invalidSnapshot(command.beforeSnapshot())
                || invalidSnapshot(command.proposedSnapshot())
                || command.beforeSnapshot().policyVersion() != command.policyBaselineVersion()
                || command.proposedSnapshot().policyVersion() != command.policyBaselineVersion()) {
            throw new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
        }
    }

    private void validateOriginal(
            CreateMaintenancePremiumQuoteCommand command,
            PremiumCalculation original) {
        if (!command.productId().equals(original.getProductId())
                || !upper(command.currency()).equals(original.getCurrency())) {
            throw new BusinessException("原计算与保全报价产品或币种不一致",
                    ProductErrorCode.PRICING_INPUT_INVALID);
        }
    }

    private boolean invalidSnapshot(SnapshotReference reference) {
        return blank(reference.storageKey()) || !hash(reference.contentHash())
                || reference.policyVersion() < 0 || reference.capturedAt() == null;
    }

    private boolean hash(String value) {
        return value != null && value.matches("(?i)[a-f0-9]{64}");
    }

    private String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
