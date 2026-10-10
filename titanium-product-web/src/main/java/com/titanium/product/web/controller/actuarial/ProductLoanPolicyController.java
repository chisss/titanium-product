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
import com.titanium.product.command.pricing.loan.CreatePolicyLoanPolicyCommand;
import com.titanium.product.web.dto.pricing.loan.CreatePolicyLoanPolicyDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 保单贷款策略管理接口（g04-03）。
 * <p>
 * 自 {@code ProductActuarialWorkbenchController} 按资源主题拆分（根规约 §3.4.14 端点上限），
 * URL 与原路径完全一致：{@code /web/v2/actuarial/products/{productId}/loan-policies}。
 * </p>
 */
@Validated
@RestController
@RequestMapping("/web/v2/actuarial/products/{productId}/loan-policies")
@RequiredArgsConstructor
public class ProductLoanPolicyController {

    private final ActuarialConfigurationCommandAppService commandAppService;

    @PostMapping
    public ApiResponse<String> createPolicyLoanPolicy(
            @PathVariable String productId,
            @Valid @RequestBody CreatePolicyLoanPolicyDTO request) {
        String tenantId = RequestContextHolder.requireTenantId();
        return ApiResponse.success(commandAppService.createPolicyLoanPolicy(
                new CreatePolicyLoanPolicyCommand(
                        tenantId, productId, request.policyCode(), request.policyVersion(),
                        request.loanableRatio(), request.interestRate(),
                        request.effectiveFrom(), request.effectiveTo())));
    }

    @PostMapping("/{policyId}/approve")
    public ApiResponse<String> approvePolicyLoanPolicy(
            @PathVariable String productId,
            @PathVariable String policyId) {
        String tenantId = RequestContextHolder.requireTenantId();
        return ApiResponse.success(commandAppService.approvePolicyLoanPolicy(tenantId, productId, policyId));
    }

    @PostMapping("/{policyId}/publish")
    public ApiResponse<Void> publishPolicyLoanPolicy(
            @PathVariable String productId,
            @PathVariable String policyId) {
        String tenantId = RequestContextHolder.requireTenantId();
        commandAppService.publishPolicyLoanPolicy(tenantId, productId, policyId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{policyId}/retire")
    public ApiResponse<Void> retirePolicyLoanPolicy(
            @PathVariable String productId,
            @PathVariable String policyId) {
        String tenantId = RequestContextHolder.requireTenantId();
        commandAppService.retirePolicyLoanPolicy(tenantId, productId, policyId);
        return ApiResponse.success(null);
    }
}
