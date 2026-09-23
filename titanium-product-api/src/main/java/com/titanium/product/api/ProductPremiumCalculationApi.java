package com.titanium.product.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
            @PathVariable String productId,
            @Valid @RequestBody PremiumCalculationRequest request);

    @GetMapping("/premium-calculations/{calculationId}")
    ApiResponse<PremiumCalculationResponse> get(
            @PathVariable String calculationId);

    @PostMapping("/premium-calculations/lifecycle-adjustments")
    ApiResponse<PremiumLifecycleAdjustmentResponse> createLifecycleAdjustment(
            @Valid @RequestBody PremiumLifecycleAdjustmentRequest request);

    /** 基于既有生命周期差额生成不可变反向差额事实。 */
    @PostMapping("/premium-lifecycle-adjustments/reversals")
    ApiResponse<PremiumLifecycleAdjustmentResponse> reverseLifecycleAdjustment(
            @Valid @RequestBody PremiumLifecycleReversalRequest request);

    @GetMapping("/premium-lifecycle-adjustments/{adjustmentId}")
    ApiResponse<PremiumLifecycleAdjustmentResponse> getLifecycleAdjustment(
            @PathVariable String adjustmentId);

    /** 基于不可变确认计算和历史 Billing 期间基线生成版本化追溯差额。 */
    @PostMapping("/premium-calculations/retroactive-period-recalculations")
    ApiResponse<RetroactivePremiumPeriodRecalculationResponse> recalculateRetroactivePeriods(
            @Valid @RequestBody RetroactivePremiumPeriodRecalculationRequest request);
}
