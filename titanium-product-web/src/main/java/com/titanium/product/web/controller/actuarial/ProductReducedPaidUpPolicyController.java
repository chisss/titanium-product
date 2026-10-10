package com.titanium.product.web.controller.actuarial;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.common.context.RequestContextHolder;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.application.command.pricing.ActuarialConfigurationCommandAppService;
import com.titanium.product.command.pricing.paidup.CreateReducedPaidUpPolicyCommand;
import com.titanium.product.web.dto.pricing.paidup.CreateReducedPaidUpPolicyDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 减额缴清净费率策略管理接口（g04-05）。
 * <p>
 * 与 {@code ProductLoanPolicyController} 同范式，按资源主题独立成类（根规约 §3.4.14 端点上限）。
 * </p>
 */
@Validated
@RestController
@RequestMapping("/web/v2/actuarial/products/{productId}/reduced-paid-up-policies")
@RequiredArgsConstructor
public class ProductReducedPaidUpPolicyController {

    private final ActuarialConfigurationCommandAppService commandAppService;

    @PostMapping
    public ApiResponse<String> createReducedPaidUpPolicy(
            @PathVariable String productId,
            @Valid @RequestBody CreateReducedPaidUpPolicyDTO request) {
        String tenantId = RequestContextHolder.requireTenantId();
        return ApiResponse.success(commandAppService.createReducedPaidUpPolicy(
                new CreateReducedPaidUpPolicyCommand(
                        tenantId, productId, request.policyCode(), request.policyVersion(),
                        request.netSinglePremiumRatePerUnit(),
                        request.effectiveFrom(), request.effectiveTo())));
    }

    @PostMapping("/{policyId}/approve")
    public ApiResponse<String> approveReducedPaidUpPolicy(
            @PathVariable String productId,
            @PathVariable String policyId) {
        String tenantId = RequestContextHolder.requireTenantId();
        return ApiResponse.success(commandAppService.approveReducedPaidUpPolicy(tenantId, productId, policyId));
    }

    @PostMapping("/{policyId}/publish")
    public ApiResponse<Void> publishReducedPaidUpPolicy(
            @PathVariable String productId,
            @PathVariable String policyId) {
        String tenantId = RequestContextHolder.requireTenantId();
        commandAppService.publishReducedPaidUpPolicy(tenantId, productId, policyId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{policyId}/retire")
    public ApiResponse<Void> retireReducedPaidUpPolicy(
            @PathVariable String productId,
            @PathVariable String policyId) {
        String tenantId = RequestContextHolder.requireTenantId();
        commandAppService.retireReducedPaidUpPolicy(tenantId, productId, policyId);
        return ApiResponse.success(null);
    }
}
