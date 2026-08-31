package com.titanium.product.application.query.pricing;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.aggregate.CalculationModelDefinition;
import com.titanium.product.aggregate.ChargeComponentDefinition;
import com.titanium.product.aggregate.DynamicFactorDefinition;
import com.titanium.product.aggregate.TaxPolicyDefinition;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.query.service.ProductQueryService;
import com.titanium.product.repository.CalculationModelRepository;
import com.titanium.product.repository.ChargeComponentRepository;
import com.titanium.product.repository.DynamicFactorRepository;
import com.titanium.product.repository.TaxPolicyRepository;

import lombok.RequiredArgsConstructor;

/**
 * 精算配置读侧应用入口。
 */
@Service
@RequiredArgsConstructor
public class ActuarialConfigurationQueryAppService {

    private final ProductQueryService productQueryService;
    private final ChargeComponentRepository chargeComponentRepository;
    private final CalculationModelRepository calculationModelRepository;
    private final TaxPolicyRepository taxPolicyRepository;
    private final DynamicFactorRepository dynamicFactorRepository;

    @Transactional(readOnly = true)
    public ChargeComponentDefinition getChargeComponent(
            String tenantId, String productId, String componentId) {
        return chargeComponentRepository.findById(tenantId, productId, componentId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.ACTUARIAL_COMPONENT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<ChargeComponentDefinition> listChargeComponents(
            String tenantId, String productId, ActuarialDefinitionStatus status) {
        requireProduct(tenantId, productId);
        return chargeComponentRepository.findAll(tenantId, productId, status);
    }

    @Transactional(readOnly = true)
    public CalculationModelDefinition getCalculationModel(String tenantId, String productId, String modelId) {
        return calculationModelRepository.findById(tenantId, productId, modelId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.ACTUARIAL_MODEL_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<CalculationModelDefinition> listCalculationModels(
            String tenantId, String productId, ActuarialDefinitionStatus status) {
        requireProduct(tenantId, productId);
        return calculationModelRepository.findAll(tenantId, productId, status);
    }

    @Transactional(readOnly = true)
    public TaxPolicyDefinition getTaxPolicy(String tenantId, String productId, String policyId) {
        return taxPolicyRepository.findById(tenantId, productId, policyId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.ACTUARIAL_COMPONENT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<TaxPolicyDefinition> listTaxPolicies(
            String tenantId, String productId, ActuarialDefinitionStatus status) {
        requireProduct(tenantId, productId);
        return taxPolicyRepository.findAll(tenantId, productId, status);
    }

    @Transactional(readOnly = true)
    public DynamicFactorDefinition getDynamicFactor(String tenantId, String productId, String factorId) {
        return dynamicFactorRepository.findById(tenantId, productId, factorId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.ACTUARIAL_COMPONENT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<DynamicFactorDefinition> listDynamicFactors(
            String tenantId, String productId, ActuarialDefinitionStatus status) {
        requireProduct(tenantId, productId);
        return dynamicFactorRepository.findAll(tenantId, productId, status);
    }

    private void requireProduct(String tenantId, String productId) {
        if (productQueryService.findProductById(productId, tenantId) == null) {
            throw new BusinessException(ProductErrorCode.PRODUCT_NOT_EXIST);
        }
    }
}
