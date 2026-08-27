package com.titanium.product.web.provider;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.ProductMaintenancePremiumQuoteApi;
import com.titanium.product.api.request.MaintenancePremiumQuoteRequest;
import com.titanium.product.api.request.UnderwritingAdjustmentRequest;
import com.titanium.product.api.response.MaintenancePremiumQuoteResponse;
import com.titanium.product.application.command.maintenance.CreateMaintenancePremiumQuoteCommand;
import com.titanium.product.application.command.maintenance.CreateMaintenancePremiumQuoteCommand.SnapshotReference;
import com.titanium.product.application.command.maintenance.ProductMaintenancePremiumQuoteCommandService;
import com.titanium.product.application.model.pricing.MaintenancePremiumQuoteResult;
import com.titanium.product.common.enums.PremiumAdjustmentType;
import com.titanium.product.common.enums.PremiumLifecycleType;
import com.titanium.product.valueobject.pricing.PremiumAdjustmentRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Product 保全版本化报价正式契约实现。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ProductMaintenancePremiumQuoteApiProvider implements ProductMaintenancePremiumQuoteApi {

    private final ProductMaintenancePremiumQuoteCommandService commandService;

    @Override
    public ApiResponse<MaintenancePremiumQuoteResponse> quote(
            String productId, @Valid MaintenancePremiumQuoteRequest request, String tenantId) {
        PremiumLifecycleType lifecycleType = PremiumLifecycleType.fromCode(request.lifecycleType());
        if (lifecycleType == null || lifecycleType == PremiumLifecycleType.REVERSAL) {
            throw new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
        }
        MaintenancePremiumQuoteResult result = commandService.quote(
                new CreateMaintenancePremiumQuoteCommand(
                        tenantId, productId, request.maintenanceId(), request.policyId(),
                        request.policyBaselineVersion(), request.itemCode(), request.productVersion(),
                        request.planVersion(), lifecycleType, snapshot(request.beforeSnapshot()),
                        snapshot(request.proposedSnapshot()), request.originalCalculationId(),
                        request.businessTime(), request.currency(), request.sumInsured(), request.age(),
                        request.gender(), request.paymentTermYears(), request.coverageTermYears(),
                        request.paymentPeriods(), request.pricingFactors(),
                        adjustments(request.underwritingAdjustments()), request.channelId(), request.policyYear(),
                        request.reason(), request.idempotencyKey(), request.payloadHash()));
        return ApiResponse.success(response(result));
    }

    private SnapshotReference snapshot(MaintenancePremiumQuoteRequest.SnapshotReferenceRequest request) {
        return new SnapshotReference(
                request.storageKey(), request.contentHash(), request.policyVersion(), request.capturedAt());
    }

    private List<PremiumAdjustmentRequest> adjustments(List<UnderwritingAdjustmentRequest> requests) {
        return requests.stream().map(request -> {
            PremiumAdjustmentType type = PremiumAdjustmentType.fromCode(request.type());
            if (type == null) {
                throw new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
            }
            return new PremiumAdjustmentRequest(
                    request.adjustmentCode(), type, request.value(), request.reason(), request.ruleVersion());
        }).toList();
    }

    private MaintenancePremiumQuoteResponse response(MaintenancePremiumQuoteResult result) {
        return new MaintenancePremiumQuoteResponse(
                result.tenantId(), result.maintenanceId(), result.policyId(), result.policyBaselineVersion(),
                result.productId(), result.productVersion(), result.planVersion(), result.itemCode(),
                result.beforeSnapshotHash(), result.proposedSnapshotHash(), result.quoteId(),
                result.quoteVersion(), result.originalCalculationId(), result.originalResultHash(),
                result.replacementCalculationId(), result.replacementResultHash(),
                result.pricingPlanVersion(), result.pricingPlanContentHash(), result.idempotencyKey(),
                result.payloadHash(), result.resultHash(), result.detailSummary(), result.direction(),
                result.amount(), result.currency(), result.quotedAt(), result.validUntil());
    }
}
