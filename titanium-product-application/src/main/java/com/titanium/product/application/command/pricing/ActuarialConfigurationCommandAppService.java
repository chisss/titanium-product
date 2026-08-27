package com.titanium.product.application.command.pricing;

import org.springframework.stereotype.Service;

import com.titanium.product.application.orchestration.pricing.ActuarialConfigurationApplicationService;

import lombok.RequiredArgsConstructor;

/**
 * 精算配置写侧应用入口。
 */
@Service
@RequiredArgsConstructor
public class ActuarialConfigurationCommandAppService {

    private final ActuarialConfigurationApplicationService orchestrationService;

    public String createChargeComponent(CreateChargeComponentCommand command) {
        return orchestrationService.createChargeComponent(command);
    }

    public String approveChargeComponent(String tenantId, String productId, String componentId) {
        return orchestrationService.approveChargeComponent(tenantId, productId, componentId);
    }

    public void publishChargeComponent(String tenantId, String productId, String componentId) {
        orchestrationService.publishChargeComponent(tenantId, productId, componentId);
    }

    public void retireChargeComponent(String tenantId, String productId, String componentId) {
        orchestrationService.retireChargeComponent(tenantId, productId, componentId);
    }

    public String createCalculationModel(CreateCalculationModelCommand command) {
        return orchestrationService.createCalculationModel(command);
    }

    public String approveCalculationModel(String tenantId, String productId, String modelId) {
        return orchestrationService.approveCalculationModel(tenantId, productId, modelId);
    }

    public void publishCalculationModel(String tenantId, String productId, String modelId) {
        orchestrationService.publishCalculationModel(tenantId, productId, modelId);
    }

    public void retireCalculationModel(String tenantId, String productId, String modelId) {
        orchestrationService.retireCalculationModel(tenantId, productId, modelId);
    }

    public String createTaxPolicy(CreateTaxPolicyCommand command) {
        return orchestrationService.createTaxPolicy(command);
    }

    public String approveTaxPolicy(String tenantId, String productId, String policyId) {
        return orchestrationService.approveTaxPolicy(tenantId, productId, policyId);
    }

    public void publishTaxPolicy(String tenantId, String productId, String policyId) {
        orchestrationService.publishTaxPolicy(tenantId, productId, policyId);
    }

    public void retireTaxPolicy(String tenantId, String productId, String policyId) {
        orchestrationService.retireTaxPolicy(tenantId, productId, policyId);
    }

    public String createDynamicFactor(CreateDynamicFactorCommand command) {
        return orchestrationService.createDynamicFactor(command);
    }

    public String approveDynamicFactor(String tenantId, String productId, String factorId) {
        return orchestrationService.approveDynamicFactor(tenantId, productId, factorId);
    }

    public void publishDynamicFactor(String tenantId, String productId, String factorId) {
        orchestrationService.publishDynamicFactor(tenantId, productId, factorId);
    }

    public void retireDynamicFactor(String tenantId, String productId, String factorId) {
        orchestrationService.retireDynamicFactor(tenantId, productId, factorId);
    }
}
