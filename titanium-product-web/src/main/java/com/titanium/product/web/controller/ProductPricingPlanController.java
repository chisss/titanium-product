package com.titanium.product.web.controller;

import java.math.RoundingMode;
import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.enums.product.PricingMode;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.application.command.pricing.PricingPlanCommandAppService;
import com.titanium.product.application.query.pricing.PricingPlanQueryAppService;
import com.titanium.product.command.pricing.CreatePricingPlanDraftCommand;
import com.titanium.product.command.pricing.ReplacePricingTestCasesCommand;
import com.titanium.product.common.enums.PricingFeatureDataType;
import com.titanium.product.common.enums.PricingPlanStatus;
import com.titanium.product.pricing.aggregate.PricingPlanDefinition;
import com.titanium.product.valueobject.pricing.calculation.CalculationModelRef;
import com.titanium.product.valueobject.pricing.commission.CommissionSchemeRef;
import com.titanium.product.valueobject.pricing.premium.TaxPolicyRef;
import com.titanium.product.valueobject.pricing.pricing.DynamicFactorRef;
import com.titanium.product.valueobject.pricing.pricing.PricingFeatureContract;
import com.titanium.product.valueobject.pricing.pricing.PricingFeatureRequirement;
import com.titanium.product.valueobject.pricing.pricing.PricingPlanValidationResult;
import com.titanium.product.valueobject.pricing.pricing.PricingRoundingRule;
import com.titanium.product.valueobject.pricing.pricing.PricingRuleArtifactRef;
import com.titanium.product.valueobject.pricing.pricing.PricingTestCase;
import com.titanium.product.valueobject.pricing.pricing.PricingTestCaseDraft;
import com.titanium.product.valueobject.rate.RateTableRef;
import com.titanium.product.web.dto.pricing.commission.CommissionSchemeRefDTO;
import com.titanium.product.web.dto.pricing.dynamicfactor.DynamicFactorRefDTO;
import com.titanium.product.web.dto.pricing.factor.PricingFeatureContractDTO;
import com.titanium.product.web.dto.pricing.factor.PricingFeatureRequirementDTO;
import com.titanium.product.web.dto.pricing.pricingplan.CreatePricingPlanDraftDTO;
import com.titanium.product.web.dto.pricing.pricingplan.PricingPlanVO;
import com.titanium.product.web.dto.pricing.pricingplan.PricingPlanValidationVO;
import com.titanium.product.web.dto.pricing.ruleartifact.PricingRuleArtifactRefDTO;
import com.titanium.product.web.dto.pricing.tax.TaxPolicyRefDTO;
import com.titanium.product.web.dto.pricing.testcase.PricingTestCaseDTO;
import com.titanium.product.web.dto.pricing.testcase.PricingTestCaseResultVO;
import com.titanium.product.web.dto.pricing.testcase.PricingTestCaseVO;
import com.titanium.product.web.dto.pricing.testcase.ReplacePricingTestCasesDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Product 定价方案后台管理接口。
 */
@Validated
@RestController
@RequestMapping("/web/v1/products/{productId}/pricing-plans")
@RequiredArgsConstructor
public class ProductPricingPlanController {

    private final PricingPlanCommandAppService commandAppService;
    private final PricingPlanQueryAppService queryAppService;

    @PostMapping
    public ApiResponse<String> createDraft(
            @PathVariable String productId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody CreatePricingPlanDraftDTO request) {
        return ApiResponse.success(commandAppService.createDraft(new CreatePricingPlanDraftCommand(
                tenantId, productId, request.productVersion(), request.planVersion(),
                parsePricingMode(request.pricingMode()), request.currency(), request.effectiveFrom(),
                request.effectiveTo(), toRateTableRef(request), toFeatureContract(request.featureContract()),
                toArtifactRef(request.artifactRef()), toCalculationModelRef(request), new PricingRoundingRule(
                        request.roundingScale(), parseRoundingMode(request.roundingMode())),
                toTaxPolicyRefs(request.taxPolicyRefs()), toCommissionSchemeRefs(request.commissionSchemeRefs()),
                toDynamicFactorRefs(request.dynamicFactorRefs()))));
    }

    @PutMapping("/{planId}/test-cases")
    public ApiResponse<Void> replaceTestCases(
            @PathVariable String productId,
            @PathVariable String planId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody ReplacePricingTestCasesDTO request) {
        commandAppService.replaceTestCases(new ReplacePricingTestCasesCommand(
                tenantId, productId, planId, request.testCases().stream().map(this::toDraft).toList()));
        return ApiResponse.success(null);
    }

    @PostMapping("/{planId}/approve")
    public ApiResponse<String> approve(
            @PathVariable String productId,
            @PathVariable String planId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ApiResponse.success(commandAppService.approve(tenantId, productId, planId));
    }

    @PostMapping("/{planId}/test-cases:run")
    public ApiResponse<PricingPlanValidationVO> runTests(
            @PathVariable String productId,
            @PathVariable String planId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ApiResponse.success(toValidation(queryAppService.runTests(tenantId, productId, planId)));
    }

    @PostMapping("/{planId}/publish")
    public ApiResponse<PricingPlanValidationVO> publish(
            @PathVariable String productId,
            @PathVariable String planId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ApiResponse.success(toValidation(commandAppService.publish(tenantId, productId, planId)));
    }

    @PostMapping("/{planId}/retire")
    public ApiResponse<Void> retire(
            @PathVariable String productId,
            @PathVariable String planId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        commandAppService.retire(tenantId, productId, planId);
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<List<PricingPlanVO>> list(
            @PathVariable String productId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(required = false) String status) {
        PricingPlanStatus parsedStatus = parseStatus(status);
        return ApiResponse.success(queryAppService.list(tenantId, productId, parsedStatus).stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/{planId}")
    public ApiResponse<PricingPlanVO> get(
            @PathVariable String productId,
            @PathVariable String planId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ApiResponse.success(toResponse(queryAppService.get(tenantId, productId, planId)));
    }

    private PricingTestCaseDraft toDraft(PricingTestCaseDTO request) {
        return new PricingTestCaseDraft(
                request.caseCode(), request.description(), request.businessTime(), request.sumInsured(),
                request.age(), request.gender(), request.paymentTermYears(), request.coverageTermYears(),
                request.paymentPeriods(), request.requestSnapshot(), request.expectedPremium(), request.tolerance());
    }

    private PricingPlanVO toResponse(PricingPlanDefinition plan) {
        return new PricingPlanVO(
                plan.planId(), plan.productId(), plan.productVersion(), plan.planVersion(), plan.mode().getCode(),
                plan.status().getCode(), plan.currency(), plan.effectiveFrom(), plan.effectiveTo(),
                plan.rateTableRef() == null ? null : plan.rateTableRef().tableCode(),
                plan.rateTableRef() == null ? null : plan.rateTableRef().version(),
                plan.artifactRef() == null ? null : plan.artifactRef().artifactCode(),
                plan.artifactRef() == null ? null : plan.artifactRef().artifactVersion(),
                plan.artifactRef() == null ? null : plan.artifactRef().inputSchemaVersion(),
                plan.artifactRef() == null ? null : plan.artifactRef().artifactHash(),
                plan.calculationModelRef() == null ? null : plan.calculationModelRef().modelCode(),
                plan.calculationModelRef() == null ? null : plan.calculationModelRef().modelVersion(),
                plan.calculationModelRef() == null ? null : plan.calculationModelRef().contentHash(),
                plan.roundingRule().scale(), plan.roundingRule().roundingMode().name(), plan.contentHash(),
                plan.testCases().stream().map(this::toResponse).toList(),
                plan.taxPolicyRefs().stream().map(ref -> new TaxPolicyRefDTO(
                        ref.policyCode(), ref.policyVersion(), ref.contentHash())).toList(),
                plan.commissionSchemeRefs().stream().map(ref -> new CommissionSchemeRefDTO(
                        ref.channelId(), ref.schemeCode(), ref.schemeVersion(), ref.contentHash())).toList(),
                plan.dynamicFactorRefs().stream().map(ref -> new DynamicFactorRefDTO(
                        ref.factorCode(), ref.factorVersion(), ref.contentHash())).toList());
    }

    private PricingTestCaseVO toResponse(PricingTestCase testCase) {
        return new PricingTestCaseVO(
                testCase.caseId(), testCase.caseCode(), testCase.description(), testCase.businessTime(),
                testCase.sumInsured(), testCase.age(), testCase.gender(), testCase.paymentTermYears(),
                testCase.coverageTermYears(), testCase.paymentPeriods(), testCase.requestSnapshot(),
                testCase.expectedPremium(), testCase.tolerance());
    }

    private PricingPlanValidationVO toValidation(PricingPlanValidationResult validation) {
        return new PricingPlanValidationVO(
                validation.planContentHash(), validation.totalCases(), validation.passedCases(),
                validation.caseResults().stream().map(result -> new PricingTestCaseResultVO(
                        result.caseCode(), result.passed(), result.expectedPremium(), result.actualPremium(),
                        result.difference(), result.failureReason())).toList());
    }

    private RateTableRef toRateTableRef(CreatePricingPlanDraftDTO request) {
        if (request.rateTableCode() == null && request.rateTableVersion() == null) {
            return null;
        }
        if (request.rateTableCode() == null || request.rateTableVersion() == null) {
            throw new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
        }
        return new RateTableRef(null, request.rateTableCode(), request.rateTableVersion(),
                request.rateDimensionKeys());
    }

    private PricingFeatureContract toFeatureContract(PricingFeatureContractDTO request) {
        if (request == null) {
            return null;
        }
        List<PricingFeatureRequirement> requirements = request.requirements() == null ? List.of()
                : request.requirements().stream().map(this::toRequirement).toList();
        return new PricingFeatureContract(request.contractId(), request.contractVersion(), requirements);
    }

    private PricingFeatureRequirement toRequirement(PricingFeatureRequirementDTO request) {
        PricingFeatureDataType dataType = PricingFeatureDataType.fromCode(request.dataType().toUpperCase());
        if (dataType == null) {
            throw new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
        }
        return new PricingFeatureRequirement(
                request.featureCode(), dataType, request.required(), request.definitionVersion(),
                request.missingPolicy(), request.sensitivity());
    }

    private PricingRuleArtifactRef toArtifactRef(PricingRuleArtifactRefDTO request) {
        return request == null ? null : new PricingRuleArtifactRef(
                request.artifactCode(), request.artifactVersion(), request.inputSchemaVersion(), request.artifactHash());
    }

    private List<TaxPolicyRef> toTaxPolicyRefs(List<TaxPolicyRefDTO> requests) {
        return requests == null ? List.of() : requests.stream()
                .map(request -> new TaxPolicyRef(
                        request.policyCode(), request.policyVersion(), request.contentHash().toLowerCase()))
                .toList();
    }

    private List<CommissionSchemeRef> toCommissionSchemeRefs(List<CommissionSchemeRefDTO> requests) {
        return requests == null ? List.of() : requests.stream()
                .map(request -> new CommissionSchemeRef(
                        request.channelId(), request.schemeCode(), request.schemeVersion(),
                        request.contentHash().toLowerCase()))
                .toList();
    }

    private List<DynamicFactorRef> toDynamicFactorRefs(List<DynamicFactorRefDTO> requests) {
        return requests == null ? List.of() : requests.stream()
                .map(request -> new DynamicFactorRef(
                        request.factorCode(), request.factorVersion(), request.contentHash().toLowerCase()))
                .toList();
    }

    private CalculationModelRef toCalculationModelRef(CreatePricingPlanDraftDTO request) {
        if (request.calculationModelCode() == null && request.calculationModelVersion() == null
                && request.calculationModelHash() == null) {
            return null;
        }
        if (request.calculationModelCode() == null || request.calculationModelVersion() == null
                || request.calculationModelHash() == null) {
            throw new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
        }
        return new CalculationModelRef(
                request.calculationModelCode(), request.calculationModelVersion(), request.calculationModelHash());
    }

    private PricingMode parsePricingMode(String value) {
        PricingMode mode = PricingMode.fromCode(value);
        if (mode == null) {
            throw new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
        }
        return mode;
    }

    private PricingPlanStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        PricingPlanStatus status = PricingPlanStatus.fromCode(value);
        if (status == null) {
            throw new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
        }
        return status;
    }

    private RoundingMode parseRoundingMode(String value) {
        try {
            return RoundingMode.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
        }
    }
}
