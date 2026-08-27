package com.titanium.product.application.query.pricing;

import java.util.List;

import org.springframework.stereotype.Service;

import com.titanium.product.aggregate.CalculationModelDefinition;
import com.titanium.product.aggregate.ChargeComponentDefinition;
import com.titanium.product.aggregate.DynamicFactorDefinition;
import com.titanium.product.aggregate.TaxPolicyDefinition;
import com.titanium.product.application.orchestration.pricing.ActuarialConfigurationApplicationService;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;

import lombok.RequiredArgsConstructor;

/**
 * 精算配置读侧应用入口。
 */
@Service
@RequiredArgsConstructor
public class ActuarialConfigurationQueryAppService {

    private final ActuarialConfigurationApplicationService orchestrationService;

    public ChargeComponentDefinition getChargeComponent(
            String tenantId, String productId, String componentId) {
        return orchestrationService.getChargeComponent(tenantId, productId, componentId);
    }

    public List<ChargeComponentDefinition> listChargeComponents(
            String tenantId, String productId, ActuarialDefinitionStatus status) {
        return orchestrationService.listChargeComponents(tenantId, productId, status);
    }

    public CalculationModelDefinition getCalculationModel(String tenantId, String productId, String modelId) {
        return orchestrationService.getCalculationModel(tenantId, productId, modelId);
    }

    public List<CalculationModelDefinition> listCalculationModels(
            String tenantId, String productId, ActuarialDefinitionStatus status) {
        return orchestrationService.listCalculationModels(tenantId, productId, status);
    }

    public TaxPolicyDefinition getTaxPolicy(String tenantId, String productId, String policyId) {
        return orchestrationService.getTaxPolicy(tenantId, productId, policyId);
    }

    public List<TaxPolicyDefinition> listTaxPolicies(
            String tenantId, String productId, ActuarialDefinitionStatus status) {
        return orchestrationService.listTaxPolicies(tenantId, productId, status);
    }

    public DynamicFactorDefinition getDynamicFactor(String tenantId, String productId, String factorId) {
        return orchestrationService.getDynamicFactor(tenantId, productId, factorId);
    }

    public List<DynamicFactorDefinition> listDynamicFactors(
            String tenantId, String productId, ActuarialDefinitionStatus status) {
        return orchestrationService.listDynamicFactors(tenantId, productId, status);
    }
}
