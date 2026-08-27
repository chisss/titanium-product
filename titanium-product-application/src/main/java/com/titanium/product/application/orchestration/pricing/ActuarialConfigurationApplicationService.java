package com.titanium.product.application.orchestration.pricing;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.aggregate.CalculationModelDefinition;
import com.titanium.product.aggregate.ChargeComponentDefinition;
import com.titanium.product.aggregate.DynamicFactorDefinition;
import com.titanium.product.aggregate.TaxPolicyDefinition;
import com.titanium.product.application.command.pricing.CreateCalculationModelCommand;
import com.titanium.product.application.command.pricing.CreateChargeComponentCommand;
import com.titanium.product.application.command.pricing.CreateDynamicFactorCommand;
import com.titanium.product.application.command.pricing.CreateTaxPolicyCommand;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.query.service.ProductQueryService;
import com.titanium.product.repository.CalculationModelRepository;
import com.titanium.product.repository.ChargeComponentRepository;
import com.titanium.product.repository.DynamicFactorRepository;
import com.titanium.product.repository.TaxPolicyRepository;

import lombok.RequiredArgsConstructor;

/**
 * Product 精算工作台配置应用编排。
 */
@Service
@RequiredArgsConstructor
public class ActuarialConfigurationApplicationService {

    private final ProductQueryService productQueryService;
    private final ChargeComponentRepository chargeComponentRepository;
    private final CalculationModelRepository calculationModelRepository;
    private final TaxPolicyRepository taxPolicyRepository;
    private final DynamicFactorRepository dynamicFactorRepository;

    @Transactional
    public String createChargeComponent(CreateChargeComponentCommand command) {
        requireProduct(command.tenantId(), command.productId());
        if (chargeComponentRepository.existsByBusinessKey(
                command.tenantId(), command.productId(), command.componentCode(), command.componentVersion())) {
            throw new BusinessException(ProductErrorCode.ACTUARIAL_COMPONENT_ALREADY_EXISTS);
        }
        ChargeComponentDefinition component = ChargeComponentDefinition.createDraft(
                UUID.randomUUID().toString(), command.productId(), command.componentCode(),
                command.componentVersion(), command.componentName(), command.description(), command.category(),
                command.amountChannel(), command.direction(), command.payerType(), command.calculationSource(),
                command.accountingClass(), command.customerVisible(), command.effectiveFrom(), command.effectiveTo(),
                command.tenantId());
        chargeComponentRepository.save(component);
        return component.getComponentId();
    }

    @Transactional
    public String approveChargeComponent(String tenantId, String productId, String componentId) {
        ChargeComponentDefinition component = requireComponent(tenantId, productId, componentId);
        String hash = component.approve();
        chargeComponentRepository.save(component);
        return hash;
    }

    @Transactional
    public void publishChargeComponent(String tenantId, String productId, String componentId) {
        ChargeComponentDefinition component = requireComponent(tenantId, productId, componentId);
        component.publish();
        chargeComponentRepository.save(component);
    }

    @Transactional
    public void retireChargeComponent(String tenantId, String productId, String componentId) {
        ChargeComponentDefinition component = requireComponent(tenantId, productId, componentId);
        component.retire();
        chargeComponentRepository.save(component);
    }

    @Transactional(readOnly = true)
    public ChargeComponentDefinition getChargeComponent(String tenantId, String productId, String componentId) {
        return requireComponent(tenantId, productId, componentId);
    }

    @Transactional(readOnly = true)
    public List<ChargeComponentDefinition> listChargeComponents(
            String tenantId, String productId, ActuarialDefinitionStatus status) {
        requireProduct(tenantId, productId);
        return chargeComponentRepository.findAll(tenantId, productId, status);
    }

    @Transactional
    public String createCalculationModel(CreateCalculationModelCommand command) {
        requireProduct(command.tenantId(), command.productId());
        if (calculationModelRepository.existsByBusinessKey(
                command.tenantId(), command.productId(), command.modelCode(), command.modelVersion())) {
            throw new BusinessException(ProductErrorCode.ACTUARIAL_MODEL_ALREADY_EXISTS);
        }
        CalculationModelDefinition model = CalculationModelDefinition.createDraft(
                UUID.randomUUID().toString(), command.productId(), command.modelCode(), command.modelVersion(),
                command.modelName(), command.description(), command.currency(), command.nodes(), command.edges(),
                command.effectiveFrom(), command.effectiveTo(), command.tenantId());
        calculationModelRepository.save(model);
        return model.getModelId();
    }

    @Transactional
    public String approveCalculationModel(String tenantId, String productId, String modelId) {
        CalculationModelDefinition model = requireModel(tenantId, productId, modelId);
        validatePublishedComponents(model);
        String hash = model.approve();
        calculationModelRepository.save(model);
        return hash;
    }

    @Transactional
    public void publishCalculationModel(String tenantId, String productId, String modelId) {
        CalculationModelDefinition model = requireModel(tenantId, productId, modelId);
        model.publish();
        calculationModelRepository.save(model);
    }

    @Transactional
    public void retireCalculationModel(String tenantId, String productId, String modelId) {
        CalculationModelDefinition model = requireModel(tenantId, productId, modelId);
        model.retire();
        calculationModelRepository.save(model);
    }

    @Transactional(readOnly = true)
    public CalculationModelDefinition getCalculationModel(String tenantId, String productId, String modelId) {
        return requireModel(tenantId, productId, modelId);
    }

    @Transactional(readOnly = true)
    public List<CalculationModelDefinition> listCalculationModels(
            String tenantId, String productId, ActuarialDefinitionStatus status) {
        requireProduct(tenantId, productId);
        return calculationModelRepository.findAll(tenantId, productId, status);
    }

    @Transactional
    public String createTaxPolicy(CreateTaxPolicyCommand command) {
        requireProduct(command.tenantId(), command.productId());
        if (taxPolicyRepository.existsByBusinessKey(
                command.tenantId(), command.productId(), command.policyCode(), command.policyVersion())) {
            throw new BusinessException(ProductErrorCode.ACTUARIAL_COMPONENT_ALREADY_EXISTS);
        }
        TaxPolicyDefinition policy = TaxPolicyDefinition.createDraft(
                UUID.randomUUID().toString(), command.productId(), command.policyCode(), command.policyVersion(),
                command.policyName(), command.description(), command.jurisdictionCode(), command.category(),
                command.payerType(), command.priceMode(), command.taxRate(), command.baseComponentCodes(),
                command.accountingClass(), command.regulatoryReferenceId(), command.exemptionFeatureCode(),
                command.effectiveFrom(), command.effectiveTo(), command.tenantId());
        taxPolicyRepository.save(policy);
        return policy.getPolicyId();
    }

    @Transactional
    public String approveTaxPolicy(String tenantId, String productId, String policyId) {
        TaxPolicyDefinition policy = requireTaxPolicy(tenantId, productId, policyId);
        validateTaxBaseComponents(policy);
        String hash = policy.approve();
        taxPolicyRepository.save(policy);
        return hash;
    }

    @Transactional
    public void publishTaxPolicy(String tenantId, String productId, String policyId) {
        TaxPolicyDefinition policy = requireTaxPolicy(tenantId, productId, policyId);
        policy.publish();
        taxPolicyRepository.save(policy);
    }

    @Transactional
    public void retireTaxPolicy(String tenantId, String productId, String policyId) {
        TaxPolicyDefinition policy = requireTaxPolicy(tenantId, productId, policyId);
        policy.retire();
        taxPolicyRepository.save(policy);
    }

    @Transactional(readOnly = true)
    public TaxPolicyDefinition getTaxPolicy(String tenantId, String productId, String policyId) {
        return requireTaxPolicy(tenantId, productId, policyId);
    }

    @Transactional(readOnly = true)
    public List<TaxPolicyDefinition> listTaxPolicies(
            String tenantId, String productId, ActuarialDefinitionStatus status) {
        requireProduct(tenantId, productId);
        return taxPolicyRepository.findAll(tenantId, productId, status);
    }

    @Transactional
    public String createDynamicFactor(CreateDynamicFactorCommand command) {
        requireProduct(command.tenantId(), command.productId());
        if (dynamicFactorRepository.existsByBusinessKey(
                command.tenantId(), command.productId(), command.factorCode(), command.factorVersion())) {
            throw new BusinessException(ProductErrorCode.ACTUARIAL_COMPONENT_ALREADY_EXISTS);
        }
        DynamicFactorDefinition factor = DynamicFactorDefinition.createDraft(
                UUID.randomUUID().toString(), command.productId(), command.factorCode(), command.factorVersion(),
                command.factorName(), command.description(), command.featureCode(),
                command.featureDefinitionVersion(), command.sourceType(), command.valueTimePolicy(),
                command.lowerBound(), command.upperBound(), command.missingPolicy(), command.defaultValue(),
                command.transformType(), command.multiplier(), command.offset(), command.replayable(),
                command.effectiveFrom(), command.effectiveTo(), command.tenantId());
        dynamicFactorRepository.save(factor);
        return factor.getFactorId();
    }

    @Transactional
    public String approveDynamicFactor(String tenantId, String productId, String factorId) {
        DynamicFactorDefinition factor = requireDynamicFactor(tenantId, productId, factorId);
        String hash = factor.approve();
        dynamicFactorRepository.save(factor);
        return hash;
    }

    @Transactional
    public void publishDynamicFactor(String tenantId, String productId, String factorId) {
        DynamicFactorDefinition factor = requireDynamicFactor(tenantId, productId, factorId);
        factor.publish();
        dynamicFactorRepository.save(factor);
    }

    @Transactional
    public void retireDynamicFactor(String tenantId, String productId, String factorId) {
        DynamicFactorDefinition factor = requireDynamicFactor(tenantId, productId, factorId);
        factor.retire();
        dynamicFactorRepository.save(factor);
    }

    @Transactional(readOnly = true)
    public DynamicFactorDefinition getDynamicFactor(String tenantId, String productId, String factorId) {
        return requireDynamicFactor(tenantId, productId, factorId);
    }

    @Transactional(readOnly = true)
    public List<DynamicFactorDefinition> listDynamicFactors(
            String tenantId, String productId, ActuarialDefinitionStatus status) {
        requireProduct(tenantId, productId);
        return dynamicFactorRepository.findAll(tenantId, productId, status);
    }

    private void validatePublishedComponents(CalculationModelDefinition model) {
        model.getNodes().stream()
                .filter(node -> node.hasComponent())
                .distinct()
                .forEach(node -> chargeComponentRepository.findPublished(
                                model.getTenantId(), model.getProductId(), node.componentCode(),
                                node.componentVersion(), model.getEffectiveFrom())
                        .orElseThrow(() -> new BusinessException(
                                "计算模型引用未发布费用项: " + node.componentCode() + '/' + node.componentVersion(),
                                ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED)));
    }

    private ChargeComponentDefinition requireComponent(String tenantId, String productId, String componentId) {
        return chargeComponentRepository.findById(tenantId, productId, componentId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.ACTUARIAL_COMPONENT_NOT_FOUND));
    }

    private CalculationModelDefinition requireModel(String tenantId, String productId, String modelId) {
        return calculationModelRepository.findById(tenantId, productId, modelId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.ACTUARIAL_MODEL_NOT_FOUND));
    }

    private TaxPolicyDefinition requireTaxPolicy(String tenantId, String productId, String policyId) {
        return taxPolicyRepository.findById(tenantId, productId, policyId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.ACTUARIAL_COMPONENT_NOT_FOUND));
    }

    private DynamicFactorDefinition requireDynamicFactor(String tenantId, String productId, String factorId) {
        return dynamicFactorRepository.findById(tenantId, productId, factorId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.ACTUARIAL_COMPONENT_NOT_FOUND));
    }

    private void validateTaxBaseComponents(TaxPolicyDefinition policy) {
        for (String componentCode : policy.getBaseComponentCodes()) {
            boolean published = chargeComponentRepository.findAll(
                            policy.getTenantId(), policy.getProductId(), ActuarialDefinitionStatus.PUBLISHED).stream()
                    .anyMatch(component -> component.getComponentCode().equalsIgnoreCase(componentCode)
                            && (component.getEffectiveFrom().equals(policy.getEffectiveFrom())
                            || component.getEffectiveFrom().isBefore(policy.getEffectiveFrom())));
            if (!published) {
                throw new BusinessException(
                        "税基引用未发布费用项: " + componentCode,
                        ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED);
            }
        }
    }

    private void requireProduct(String tenantId, String productId) {
        if (productQueryService.findProductById(productId, tenantId) == null) {
            throw new BusinessException(ProductErrorCode.PRODUCT_NOT_EXIST);
        }
    }
}
