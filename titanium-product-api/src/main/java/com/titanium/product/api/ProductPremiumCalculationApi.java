package com.titanium.product.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.request.premium.PremiumCalculationRequest;
import com.titanium.product.api.request.premium.PremiumLifecycleAdjustmentRequest;
import com.titanium.product.api.request.premium.PremiumLifecycleReversalRequest;
import com.titanium.product.api.request.premium.RetroactivePremiumPeriodRecalculationRequest;
import com.titanium.product.api.response.premium.PremiumCalculationResponse;
import com.titanium.product.api.response.premium.PremiumLifecycleAdjustmentResponse;
import com.titanium.product.api.response.premium.RetroactivePremiumPeriodRecalculationResponse;

import jakarta.validation.Valid;

/**
 * Product 确认计算远程契约。
 */
@FeignClient(name = "titanium-product-service", contextId = "productPremiumCalculationApi", path = "/api/v1")
public interface ProductPremiumCalculationApi {

    @PostMapping("/products/{productId}/premium-calculations")
    ApiResponse<PremiumCalculationResponse> confirm(
            @PathVariable("productId") String productId,
            @Valid @RequestBody PremiumCalculationRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId);

    @GetMapping("/premium-calculations/{calculationId}")
    ApiResponse<PremiumCalculationResponse> get(
            @PathVariable("calculationId") String calculationId,
            @RequestHeader("X-Tenant-ID") String tenantId);

    @PostMapping("/premium-calculations/lifecycle-adjustments")
    ApiResponse<PremiumLifecycleAdjustmentResponse> createLifecycleAdjustment(
            @Valid @RequestBody PremiumLifecycleAdjustmentRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId);

    /** 基于既有生命周期差额生成不可变反向差额事实。 */
    @PostMapping("/premium-lifecycle-adjustments/reversals")
    ApiResponse<PremiumLifecycleAdjustmentResponse> reverseLifecycleAdjustment(
            @Valid @RequestBody PremiumLifecycleReversalRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId);

    @GetMapping("/premium-lifecycle-adjustments/{adjustmentId}")
    ApiResponse<PremiumLifecycleAdjustmentResponse> getLifecycleAdjustment(
            @PathVariable("adjustmentId") String adjustmentId,
            @RequestHeader("X-Tenant-ID") String tenantId);

    /** 基于不可变确认计算和历史 Billing 期间基线生成版本化追溯差额。 */
    @PostMapping("/premium-calculations/retroactive-period-recalculations")
    ApiResponse<RetroactivePremiumPeriodRecalculationResponse> recalculateRetroactivePeriods(
            @Valid @RequestBody RetroactivePremiumPeriodRecalculationRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId);
}
