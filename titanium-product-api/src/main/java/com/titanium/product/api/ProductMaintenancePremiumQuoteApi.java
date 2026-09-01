package com.titanium.product.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.request.premium.MaintenancePremiumQuoteRequest;
import com.titanium.product.api.response.premium.MaintenancePremiumQuoteResponse;

import jakarta.validation.Valid;

/** Product 保全版本化报价正式契约。 */
@FeignClient(
        name = "titanium-product-service",
        contextId = "productMaintenancePremiumQuoteApi",
        path = "/api/v1")
public interface ProductMaintenancePremiumQuoteApi {

    @PostMapping("/products/{productId}/maintenance-premium-quotes")
    ApiResponse<MaintenancePremiumQuoteResponse> quote(
            @PathVariable("productId") String productId,
            @Valid @RequestBody MaintenancePremiumQuoteRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId);
}
