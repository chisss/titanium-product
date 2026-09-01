package com.titanium.product.web.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.application.command.pricing.ActuarialConfigurationCommandAppService;
import com.titanium.product.application.query.pricing.ActuarialConfigurationQueryAppService;
import com.titanium.product.command.pricing.CreateCalculationModelCommand;
import com.titanium.product.command.pricing.CreateChargeComponentCommand;
import com.titanium.product.command.pricing.CreateDynamicFactorCommand;
import com.titanium.product.command.pricing.CreateTaxPolicyCommand;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.common.enums.CalculationNodeType;
import com.titanium.product.common.enums.CalculationOperator;
import com.titanium.product.common.enums.ChargeCalculationSource;
import com.titanium.product.common.enums.DynamicFactorMissingPolicy;
import com.titanium.product.common.enums.DynamicFactorSourceType;
import com.titanium.product.common.enums.DynamicFactorTransformType;
import com.titanium.product.common.enums.DynamicFactorValueTimePolicy;
import com.titanium.product.common.enums.TaxPriceMode;
import com.titanium.product.pricing.aggregate.CalculationModelDefinition;
import com.titanium.product.pricing.aggregate.ChargeComponentDefinition;
import com.titanium.product.pricing.aggregate.DynamicFactorDefinition;
import com.titanium.product.pricing.aggregate.TaxPolicyDefinition;
import com.titanium.product.valueobject.pricing.calculation.CalculationEdge;
import com.titanium.product.valueobject.pricing.calculation.CalculationNode;
import com.titanium.product.web.dto.pricing.calculation.CalculationEdgeDTO;
import com.titanium.product.web.dto.pricing.calculation.CalculationModelVO;
import com.titanium.product.web.dto.pricing.calculation.CalculationNodeDTO;
import com.titanium.product.web.dto.pricing.calculation.ChargeComponentVO;
import com.titanium.product.web.dto.pricing.calculation.CreateCalculationModelDTO;
import com.titanium.product.web.dto.pricing.calculation.CreateChargeComponentDTO;
import com.titanium.product.web.dto.pricing.dynamicfactor.CreateDynamicFactorDTO;
import com.titanium.product.web.dto.pricing.dynamicfactor.DynamicFactorVO;
import com.titanium.product.web.dto.pricing.tax.CreateTaxPolicyDTO;
import com.titanium.product.web.dto.pricing.tax.TaxPolicyVO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Product V2 精算工作台管理接口。
 */
@Validated
@RestController
@RequestMapping("/web/v2/actuarial/products/{productId}")
@RequiredArgsConstructor
public class ProductActuarialWorkbenchController {

    private final ActuarialConfigurationCommandAppService commandAppService;
    private final ActuarialConfigurationQueryAppService queryAppService;

    @PostMapping("/charge-components")
    public ApiResponse<String> createChargeComponent(
            @PathVariable String productId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody CreateChargeComponentDTO request) {
        return ApiResponse.success(commandAppService.createChargeComponent(new CreateChargeComponentCommand(
                tenantId, productId, request.componentCode(), request.componentVersion(), request.componentName(),
                request.description(), require(ChargeCategory.fromCode(request.category())),
                require(AmountChannel.fromCode(request.amountChannel())),
                require(ChargeDirection.fromCode(request.direction())),
                require(ChargePayerType.fromCode(request.payerType())),
                require(ChargeCalculationSource.fromCode(request.calculationSource())),
                request.accountingClass(), request.customerVisible(), request.effectiveFrom(), request.effectiveTo())));
    }

    @GetMapping("/charge-components")
    public ApiResponse<List<ChargeComponentVO>> listChargeComponents(
            @PathVariable String productId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(queryAppService.listChargeComponents(
                        tenantId, productId, parseStatus(status)).stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/charge-components/{componentId}")
    public ApiResponse<ChargeComponentVO> getChargeComponent(
            @PathVariable String productId,
            @PathVariable String componentId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ApiResponse.success(toResponse(
                queryAppService.getChargeComponent(tenantId, productId, componentId)));
    }

    @PostMapping("/charge-components/{componentId}/approve")
    public ApiResponse<String> approveChargeComponent(
            @PathVariable String productId,
            @PathVariable String componentId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ApiResponse.success(commandAppService.approveChargeComponent(tenantId, productId, componentId));
    }

    @PostMapping("/charge-components/{componentId}/publish")
    public ApiResponse<Void> publishChargeComponent(
            @PathVariable String productId,
            @PathVariable String componentId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        commandAppService.publishChargeComponent(tenantId, productId, componentId);
        return ApiResponse.success(null);
    }

    @PostMapping("/charge-components/{componentId}/retire")
    public ApiResponse<Void> retireChargeComponent(
            @PathVariable String productId,
            @PathVariable String componentId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        commandAppService.retireChargeComponent(tenantId, productId, componentId);
        return ApiResponse.success(null);
    }

    @PostMapping("/calculation-models")
    public ApiResponse<String> createCalculationModel(
            @PathVariable String productId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody CreateCalculationModelDTO request) {
        return ApiResponse.success(commandAppService.createCalculationModel(new CreateCalculationModelCommand(
                tenantId, productId, request.modelCode(), request.modelVersion(), request.modelName(),
                request.description(), request.currency(), request.nodes().stream().map(this::toDomain).toList(),
                request.edges() == null ? List.of() : request.edges().stream().map(this::toDomain).toList(),
                request.effectiveFrom(), request.effectiveTo())));
    }

    @GetMapping("/calculation-models")
    public ApiResponse<List<CalculationModelVO>> listCalculationModels(
            @PathVariable String productId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(queryAppService.listCalculationModels(
                        tenantId, productId, parseStatus(status)).stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/calculation-models/{modelId}")
    public ApiResponse<CalculationModelVO> getCalculationModel(
            @PathVariable String productId,
            @PathVariable String modelId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ApiResponse.success(toResponse(
                queryAppService.getCalculationModel(tenantId, productId, modelId)));
    }

    @PostMapping("/calculation-models/{modelId}/approve")
    public ApiResponse<String> approveCalculationModel(
            @PathVariable String productId,
            @PathVariable String modelId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ApiResponse.success(commandAppService.approveCalculationModel(tenantId, productId, modelId));
    }

    @PostMapping("/calculation-models/{modelId}/publish")
    public ApiResponse<Void> publishCalculationModel(
            @PathVariable String productId,
            @PathVariable String modelId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        commandAppService.publishCalculationModel(tenantId, productId, modelId);
        return ApiResponse.success(null);
    }

    @PostMapping("/calculation-models/{modelId}/retire")
    public ApiResponse<Void> retireCalculationModel(
            @PathVariable String productId,
            @PathVariable String modelId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        commandAppService.retireCalculationModel(tenantId, productId, modelId);
        return ApiResponse.success(null);
    }

    @PostMapping("/tax-policies")
    public ApiResponse<String> createTaxPolicy(
            @PathVariable String productId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody CreateTaxPolicyDTO request) {
        return ApiResponse.success(commandAppService.createTaxPolicy(new CreateTaxPolicyCommand(
                tenantId, productId, request.policyCode(), request.policyVersion(), request.policyName(),
                request.description(), request.jurisdictionCode(), require(ChargeCategory.fromCode(request.category())),
                require(ChargePayerType.fromCode(request.payerType())),
                require(TaxPriceMode.fromCode(request.priceMode())), request.taxRate(), request.baseComponentCodes(),
                request.accountingClass(), request.regulatoryReferenceId(), request.exemptionFeatureCode(),
                request.effectiveFrom(), request.effectiveTo())));
    }

    @GetMapping("/tax-policies")
    public ApiResponse<List<TaxPolicyVO>> listTaxPolicies(
            @PathVariable String productId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(queryAppService.listTaxPolicies(tenantId, productId, parseStatus(status)).stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/tax-policies/{policyId}")
    public ApiResponse<TaxPolicyVO> getTaxPolicy(
            @PathVariable String productId,
            @PathVariable String policyId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ApiResponse.success(toResponse(queryAppService.getTaxPolicy(tenantId, productId, policyId)));
    }

    @PostMapping("/tax-policies/{policyId}/approve")
    public ApiResponse<String> approveTaxPolicy(
            @PathVariable String productId,
            @PathVariable String policyId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ApiResponse.success(commandAppService.approveTaxPolicy(tenantId, productId, policyId));
    }

    @PostMapping("/tax-policies/{policyId}/publish")
    public ApiResponse<Void> publishTaxPolicy(
            @PathVariable String productId,
            @PathVariable String policyId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        commandAppService.publishTaxPolicy(tenantId, productId, policyId);
        return ApiResponse.success(null);
    }

    @PostMapping("/tax-policies/{policyId}/retire")
    public ApiResponse<Void> retireTaxPolicy(
            @PathVariable String productId,
            @PathVariable String policyId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        commandAppService.retireTaxPolicy(tenantId, productId, policyId);
        return ApiResponse.success(null);
    }

    @PostMapping("/dynamic-factors")
    public ApiResponse<String> createDynamicFactor(
            @PathVariable String productId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody CreateDynamicFactorDTO request) {
        return ApiResponse.success(commandAppService.createDynamicFactor(new CreateDynamicFactorCommand(
                tenantId, productId, request.factorCode(), request.factorVersion(), request.factorName(),
                request.description(), request.featureCode(), request.featureDefinitionVersion(),
                require(DynamicFactorSourceType.fromCode(request.sourceType())),
                require(DynamicFactorValueTimePolicy.fromCode(request.valueTimePolicy())), request.lowerBound(),
                request.upperBound(), require(DynamicFactorMissingPolicy.fromCode(request.missingPolicy())),
                request.defaultValue(), require(DynamicFactorTransformType.fromCode(request.transformType())),
                request.multiplier(), request.offset(), request.replayable(), request.effectiveFrom(),
                request.effectiveTo())));
    }

    @GetMapping("/dynamic-factors")
    public ApiResponse<List<DynamicFactorVO>> listDynamicFactors(
            @PathVariable String productId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(queryAppService.listDynamicFactors(tenantId, productId, parseStatus(status))
                .stream().map(this::toResponse).toList());
    }

    @GetMapping("/dynamic-factors/{factorId}")
    public ApiResponse<DynamicFactorVO> getDynamicFactor(
            @PathVariable String productId,
            @PathVariable String factorId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ApiResponse.success(toResponse(queryAppService.getDynamicFactor(tenantId, productId, factorId)));
    }

    @PostMapping("/dynamic-factors/{factorId}/approve")
    public ApiResponse<String> approveDynamicFactor(
            @PathVariable String productId,
            @PathVariable String factorId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ApiResponse.success(commandAppService.approveDynamicFactor(tenantId, productId, factorId));
    }

    @PostMapping("/dynamic-factors/{factorId}/publish")
    public ApiResponse<Void> publishDynamicFactor(
            @PathVariable String productId,
            @PathVariable String factorId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        commandAppService.publishDynamicFactor(tenantId, productId, factorId);
        return ApiResponse.success(null);
    }

    @PostMapping("/dynamic-factors/{factorId}/retire")
    public ApiResponse<Void> retireDynamicFactor(
            @PathVariable String productId,
            @PathVariable String factorId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        commandAppService.retireDynamicFactor(tenantId, productId, factorId);
        return ApiResponse.success(null);
    }

    private CalculationNode toDomain(CalculationNodeDTO request) {
        return new CalculationNode(
                request.nodeCode(), request.nodeName(), require(CalculationNodeType.fromCode(request.nodeType())),
                require(CalculationOperator.fromCode(request.operator())), request.componentCode(),
                request.componentVersion(), request.parameterValue(), request.executionOrder());
    }

    private CalculationEdge toDomain(CalculationEdgeDTO request) {
        return new CalculationEdge(request.fromNodeCode(), request.toNodeCode());
    }

    private ChargeComponentVO toResponse(ChargeComponentDefinition component) {
        return new ChargeComponentVO(
                component.getComponentId(), component.getProductId(), component.getComponentCode(),
                component.getComponentVersion(), component.getComponentName(), component.getDescription(),
                component.getCategory().getCode(), component.getAmountChannel().getCode(),
                component.getDirection().getCode(), component.getPayerType().getCode(),
                component.getCalculationSource().getCode(), component.getAccountingClass(),
                component.isCustomerVisible(), component.getEffectiveFrom(), component.getEffectiveTo(),
                component.getStatus().getCode(), component.getContentHash());
    }

    private CalculationModelVO toResponse(CalculationModelDefinition model) {
        return new CalculationModelVO(
                model.getModelId(), model.getProductId(), model.getModelCode(), model.getModelVersion(),
                model.getModelName(), model.getDescription(), model.getCurrency(), model.getNodes().stream()
                        .map(node -> new CalculationNodeDTO(
                                node.nodeCode(), node.nodeName(), node.nodeType().getCode(),
                                node.operator().getCode(), node.componentCode(), node.componentVersion(),
                                node.parameterValue(), node.executionOrder()))
                        .toList(),
                model.getEdges().stream().map(edge -> new CalculationEdgeDTO(
                        edge.fromNodeCode(), edge.toNodeCode())).toList(),
                model.getEffectiveFrom(), model.getEffectiveTo(), model.getStatus().getCode(), model.getContentHash());
    }

    private TaxPolicyVO toResponse(TaxPolicyDefinition policy) {
        return new TaxPolicyVO(
                policy.getPolicyId(), policy.getProductId(), policy.getPolicyCode(), policy.getPolicyVersion(),
                policy.getPolicyName(), policy.getDescription(), policy.getJurisdictionCode(),
                policy.getCategory().getCode(), policy.getPayerType().getCode(), policy.getPriceMode().getCode(),
                policy.getTaxRate(), policy.getBaseComponentCodes(), policy.getAccountingClass(),
                policy.getRegulatoryReferenceId(), policy.getExemptionFeatureCode(), policy.getEffectiveFrom(),
                policy.getEffectiveTo(), policy.getStatus().getCode(), policy.getContentHash());
    }

    private DynamicFactorVO toResponse(DynamicFactorDefinition factor) {
        return new DynamicFactorVO(
                factor.getFactorId(), factor.getProductId(), factor.getFactorCode(), factor.getFactorVersion(),
                factor.getFactorName(), factor.getDescription(), factor.getFeatureCode(),
                factor.getFeatureDefinitionVersion(), factor.getSourceType().getCode(),
                factor.getValueTimePolicy().getCode(), factor.getLowerBound(), factor.getUpperBound(),
                factor.getMissingPolicy().getCode(), factor.getDefaultValue(), factor.getTransformType().getCode(),
                factor.getMultiplier(), factor.getOffset(), factor.isReplayable(), factor.getEffectiveFrom(),
                factor.getEffectiveTo(), factor.getStatus().getCode(), factor.getContentHash());
    }

    private ActuarialDefinitionStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return require(ActuarialDefinitionStatus.fromCode(value));
    }

    private <T> T require(T value) {
        if (value == null) {
            throw new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
        }
        return value;
    }
}
