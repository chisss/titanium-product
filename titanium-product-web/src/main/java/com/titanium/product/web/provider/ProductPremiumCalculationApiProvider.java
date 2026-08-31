package com.titanium.product.web.provider;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.aggregate.PremiumCalculation;
import com.titanium.product.aggregate.lifecycle.PremiumLifecycleAdjustment;
import com.titanium.product.api.ProductPremiumCalculationApi;
import com.titanium.product.api.request.PremiumCalculationRequest;
import com.titanium.product.api.request.PremiumLifecycleAdjustmentRequest;
import com.titanium.product.api.request.PremiumLifecycleReversalRequest;
import com.titanium.product.api.request.RetroactivePremiumPeriodRecalculationRequest;
import com.titanium.product.api.request.UnderwritingAdjustmentRequest;
import com.titanium.product.api.response.CalculationLineResponse;
import com.titanium.product.api.response.CalculationTotalsResponse;
import com.titanium.product.api.response.DynamicFactorEvidenceResponse;
import com.titanium.product.api.response.PremiumAdjustmentResponse;
import com.titanium.product.api.response.PremiumCalculationResponse;
import com.titanium.product.api.response.PremiumLifecycleAdjustmentResponse;
import com.titanium.product.api.response.PremiumLifecycleDifferenceLineResponse;
import com.titanium.product.api.response.RetroactivePremiumPeriodRecalculationResponse;
import com.titanium.product.api.response.RetroactivePremiumPeriodRecalculationResponse.PeriodDifferenceResponse;
import com.titanium.product.application.command.pricing.PremiumCalculationCommandAppService;
import com.titanium.product.application.command.pricing.lifecycle.PremiumLifecycleAdjustmentCommandAppService;
import com.titanium.product.application.command.pricing.lifecycle.RetroactivePremiumPeriodRecalculationCommandAppService;
import com.titanium.product.application.model.RetroactivePremiumPeriodRecalculationResult;
import com.titanium.product.application.query.pricing.PremiumCalculationQuery;
import com.titanium.product.application.query.pricing.PremiumCalculationQueryAppService;
import com.titanium.product.application.query.pricing.lifecycle.PremiumLifecycleAdjustmentQueryAppService;
import com.titanium.product.command.pricing.PremiumCalculationCommand;
import com.titanium.product.command.pricing.lifecycle.CreatePremiumLifecycleAdjustmentCommand;
import com.titanium.product.command.pricing.lifecycle.CreatePremiumLifecycleReversalCommand;
import com.titanium.product.command.pricing.lifecycle.RecalculateRetroactivePremiumPeriodsCommand;
import com.titanium.product.common.enums.PremiumAdjustmentType;
import com.titanium.product.common.enums.PremiumLifecycleType;
import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.valueobject.pricing.PremiumAdjustmentRequest;

import jakarta.validation.Valid;

/**
 * Product 确认计算契约实现。
 */
@Validated
@RestController
@RequestMapping("/api/v1")
public class ProductPremiumCalculationApiProvider implements ProductPremiumCalculationApi {

    private final PremiumCalculationCommandAppService calculationCommandAppService;
    private final PremiumCalculationQueryAppService calculationQueryAppService;
    private final PremiumLifecycleAdjustmentCommandAppService lifecycleAdjustmentCommandAppService;
    private final PremiumLifecycleAdjustmentQueryAppService lifecycleAdjustmentQueryAppService;
    private final RetroactivePremiumPeriodRecalculationCommandAppService retroactivePeriodService;

    @Autowired
    public ProductPremiumCalculationApiProvider(
            PremiumCalculationCommandAppService calculationCommandAppService,
            PremiumCalculationQueryAppService calculationQueryAppService,
            PremiumLifecycleAdjustmentCommandAppService lifecycleAdjustmentCommandAppService,
            PremiumLifecycleAdjustmentQueryAppService lifecycleAdjustmentQueryAppService,
            RetroactivePremiumPeriodRecalculationCommandAppService retroactivePeriodService) {
        this.calculationCommandAppService = calculationCommandAppService;
        this.calculationQueryAppService = calculationQueryAppService;
        this.lifecycleAdjustmentCommandAppService = lifecycleAdjustmentCommandAppService;
        this.lifecycleAdjustmentQueryAppService = lifecycleAdjustmentQueryAppService;
        this.retroactivePeriodService = retroactivePeriodService;
    }

    ProductPremiumCalculationApiProvider(
            PremiumCalculationCommandAppService calculationCommandAppService,
            PremiumCalculationQueryAppService calculationQueryAppService,
            PremiumLifecycleAdjustmentCommandAppService lifecycleAdjustmentCommandAppService,
            PremiumLifecycleAdjustmentQueryAppService lifecycleAdjustmentQueryAppService) {
        this(calculationCommandAppService, calculationQueryAppService, lifecycleAdjustmentCommandAppService,
                lifecycleAdjustmentQueryAppService, null);
    }

    /**
     * 兼容生命周期差额能力引入前的独立 Controller 单测。
     */
    ProductPremiumCalculationApiProvider(
            PremiumCalculationCommandAppService calculationCommandAppService,
            PremiumCalculationQueryAppService calculationQueryAppService) {
        this(calculationCommandAppService, calculationQueryAppService, null, null, null);
    }

    @Override
    public ApiResponse<PremiumCalculationResponse> confirm(
            String productId, @Valid PremiumCalculationRequest request, String tenantId) {
        PricingCalculationPurpose purpose = PricingCalculationPurpose.fromCode(request.purpose());
        if (purpose == null) {
            throw new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
        }
        PremiumCalculation calculation = calculationCommandAppService.confirm(new PremiumCalculationCommand(
                tenantId, productId, request.calculationRequestId(), request.bizNo(), purpose,
                request.productVersion(), request.businessTime(), request.currency(), request.sumInsured(),
                request.age(), request.gender(), request.paymentTermYears(), request.coverageTermYears(),
                request.paymentPeriods(), request.requestSnapshot(), toDomainAdjustments(request.underwritingAdjustments()),
                request.channelId(), request.policyYear() == null ? 1 : request.policyYear()));
        return ApiResponse.success(toResponse(calculation));
    }

    @Override
    public ApiResponse<PremiumCalculationResponse> get(String calculationId, String tenantId) {
        return ApiResponse.success(toResponse(calculationQueryAppService.get(
                new PremiumCalculationQuery(tenantId, calculationId))));
    }

    @Override
    public ApiResponse<PremiumLifecycleAdjustmentResponse> createLifecycleAdjustment(
            @Valid PremiumLifecycleAdjustmentRequest request, String tenantId) {
        PremiumLifecycleType lifecycleType = PremiumLifecycleType.fromCode(request.lifecycleType());
        if (lifecycleType == null) {
            throw new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
        }
        PremiumLifecycleAdjustment adjustment = lifecycleAdjustmentCommandAppService.create(
                new CreatePremiumLifecycleAdjustmentCommand(
                        tenantId, request.adjustmentRequestId(), request.bizNo(), lifecycleType,
                        request.originalCalculationId(), request.replacementCalculationId(),
                        request.businessTime(), request.reason()));
        return ApiResponse.success(toResponse(adjustment));
    }

    @Override
    public ApiResponse<PremiumLifecycleAdjustmentResponse> reverseLifecycleAdjustment(
            @Valid PremiumLifecycleReversalRequest request, String tenantId) {
        PremiumLifecycleAdjustment reversal = lifecycleAdjustmentCommandAppService.createReversal(
                new CreatePremiumLifecycleReversalCommand(tenantId, request.adjustmentRequestId(),
                        request.sourceAdjustmentId(), request.businessTime(), request.reason()));
        return ApiResponse.success(toResponse(reversal));
    }

    @Override
    public ApiResponse<PremiumLifecycleAdjustmentResponse> getLifecycleAdjustment(
            String adjustmentId, String tenantId) {
        return ApiResponse.success(toResponse(lifecycleAdjustmentQueryAppService.get(tenantId, adjustmentId)));
    }

    @Override
    public ApiResponse<RetroactivePremiumPeriodRecalculationResponse> recalculateRetroactivePeriods(
            @Valid RetroactivePremiumPeriodRecalculationRequest request, String tenantId) {
        RetroactivePremiumPeriodRecalculationResult result = retroactivePeriodService.recalculate(
                new RecalculateRetroactivePremiumPeriodsCommand(
                        tenantId, request.recalculationRequestId(), request.maintenanceId(), request.policyId(),
                        request.analysisId(), request.analysisVersion(), request.analysisResultHash(),
                        request.originalCalculationId(), request.replacementCalculationId(),
                        request.scopeFrom(), request.scopeTo(), request.periods().stream()
                                .map(period -> new RecalculateRetroactivePremiumPeriodsCommand.AffectedPeriod(
                                        period.periodId(), period.sourceReferenceId(), period.periodStart(),
                                        period.originalAmount(), period.currency(), period.sourceEvidenceHash()))
                                .toList()));
        return ApiResponse.success(toResponse(result));
    }

    private List<PremiumAdjustmentRequest> toDomainAdjustments(
            List<UnderwritingAdjustmentRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream().map(request -> {
            PremiumAdjustmentType type = PremiumAdjustmentType.fromCode(request.type());
            if (type == null) {
                throw new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
            }
            return new PremiumAdjustmentRequest(
                    request.adjustmentCode(), type, request.value(), request.reason(), request.ruleVersion());
        }).toList();
    }

    private PremiumCalculationResponse toResponse(PremiumCalculation calculation) {
        var evidence = calculation.getEvidence();
        return new PremiumCalculationResponse(
                calculation.getCalculationId(), calculation.getCalculationRequestId(), calculation.getBizNo(),
                calculation.getPurpose().getCode(), calculation.getStatus().getCode(), calculation.getProductId(),
                evidence.productVersion(), calculation.getCurrency(), calculation.getStandardPremium(),
                calculation.getTotalPremium(), calculation.getInstallmentAmount(), calculation.getPeriods(),
                calculation.getAdjustments().stream().map(adjustment -> new PremiumAdjustmentResponse(
                        adjustment.adjustmentCode(), adjustment.type().getCode(), adjustment.value(),
                        adjustment.adjustmentAmount(), adjustment.premiumAfter(), adjustment.reason(),
                        adjustment.ruleVersion())).toList(),
                evidence.pricingPlanVersion(), evidence.pricingPlanContentHash(), evidence.rateTableCode(),
                evidence.rateTableVersion(), evidence.rateTableContentHash(), evidence.featureSnapshotId(),
                evidence.ruleArtifactCode(), evidence.ruleArtifactVersion(), evidence.ruleArtifactHash(),
                calculation.getRequestHash(), calculation.getInputHash(), calculation.getResultHash(),
                calculation.getCreatedAt(), new CalculationTotalsResponse(
                        calculation.getCalculationTotals().premiumSubtotal(),
                        calculation.getCalculationTotals().taxAndLevyTotal(),
                        calculation.getCalculationTotals().customerPayable(),
                        calculation.getCalculationTotals().internalCostTotal()),
                calculation.getCalculationLines().stream().map(line -> new CalculationLineResponse(
                        line.lineId(), line.componentCode(), line.componentVersion(), line.category().getCode(),
                        line.amountChannel().getCode(), line.direction().getCode(), line.payerType().getCode(),
                        line.accountingClass(), line.currency(), line.baseAmount(), line.rate(),
                        line.calculatedAmount(), line.nodeCode(), line.customerVisible(), line.description(),
                        line.affectsCustomerPayable(),
                        line.taxEvidence() == null ? null : line.taxEvidence().jurisdictionCode(),
                        line.taxEvidence() == null ? null : line.taxEvidence().regulatoryReferenceId(),
                        line.taxEvidence() == null ? null : line.taxEvidence().priceMode().name(),
                        line.taxEvidence() == null ? null : line.taxEvidence().policyHash(),
                        line.taxEvidence() == null ? null : line.taxEvidence().exempt(),
                        line.commissionEvidence() == null ? null : line.commissionEvidence().channelId(),
                        line.commissionEvidence() == null ? null : line.commissionEvidence().schemeCode(),
                        line.commissionEvidence() == null ? null : line.commissionEvidence().schemeVersion(),
                        line.commissionEvidence() == null ? null : line.commissionEvidence().schemeHash(),
                        line.commissionEvidence() == null ? null : line.commissionEvidence().beneficiaryType(),
                        line.commissionEvidence() == null ? null : line.commissionEvidence().beneficiaryId(),
                        line.commissionEvidence() == null ? null : line.commissionEvidence().splitRate(),
                        line.commissionEvidence() == null ? null : line.commissionEvidence().grossCommission(),
                        line.commissionEvidence() == null ? null : line.commissionEvidence().installmentCount(),
                        line.commissionEvidence() == null ? null : line.commissionEvidence().clawbackMonths())).toList(),
                evidence.calculationModelCode(), evidence.calculationModelVersion(), evidence.calculationModelHash(),
                evidence.dynamicFactorEvidence().stream().map(item -> new DynamicFactorEvidenceResponse(
                        item.factorCode(), item.factorVersion(), item.contentHash(), item.featureCode(),
                        item.featureDefinitionVersion())).toList());
    }

    private PremiumLifecycleAdjustmentResponse toResponse(PremiumLifecycleAdjustment adjustment) {
        return new PremiumLifecycleAdjustmentResponse(
                adjustment.getAdjustmentId(), adjustment.getAdjustmentRequestId(), adjustment.getBizNo(),
                adjustment.getLifecycleType().getCode(), adjustment.getProductId(),
                adjustment.getOriginalCalculationId(), adjustment.getOriginalResultHash(),
                adjustment.getReplacementCalculationId(), adjustment.getReplacementResultHash(),
                adjustment.getBusinessTime(), adjustment.getCurrency(), adjustment.getDirection().getCode(),
                adjustment.getCustomerAmount(), adjustment.getTaxDirection().getCode(), adjustment.getTaxAmount(),
                adjustment.getInternalCostDirection().getCode(), adjustment.getInternalCostAmount(),
                adjustment.getLines().stream().map(line -> new PremiumLifecycleDifferenceLineResponse(
                        line.lineId(), line.componentCode(), line.originalComponentVersion(),
                        line.replacementComponentVersion(), line.category().getCode(),
                        line.amountChannel().getCode(), line.direction().getCode(), line.payerType().getCode(),
                        line.accountingClass(), line.currency(),
                        line.originalDirection() == null ? null : line.originalDirection().getCode(),
                        line.beforeAmount(),
                        line.replacementDirection() == null ? null : line.replacementDirection().getCode(),
                        line.afterAmount(), line.differenceAmount(), line.customerVisible(),
                        line.affectsCustomerPayable(), line.description())).toList(),
                adjustment.getReason(), adjustment.getRequestHash(), adjustment.getResultHash(),
                adjustment.getCreatedAt(), adjustment.getReversalOfAdjustmentId());
    }

    private RetroactivePremiumPeriodRecalculationResponse toResponse(
            RetroactivePremiumPeriodRecalculationResult result) {
        return new RetroactivePremiumPeriodRecalculationResponse(
                result.tenantId(), result.recalculationId(), result.recalculationVersion(),
                result.recalculationRequestId(), result.maintenanceId(), result.policyId(), result.analysisId(),
                result.analysisVersion(), result.analysisResultHash(), result.productId(), result.productVersion(),
                result.originalCalculationId(), result.originalResultHash(), result.replacementCalculationId(),
                result.replacementResultHash(), result.scopeFrom(), result.scopeTo(), result.direction().getCode(),
                result.amount(), result.currency(), result.inputHash(), result.resultHash(), result.calculatedAt(),
                result.periods().stream().map(period -> new PeriodDifferenceResponse(
                        period.periodId(), period.sourceReferenceId(), period.periodStart(), period.originalAmount(),
                        period.recalculatedAmount(), period.direction().getCode(), period.differenceAmount(),
                        period.currency(), period.sourceEvidenceHash(), period.resultHash())).toList());
    }
}
