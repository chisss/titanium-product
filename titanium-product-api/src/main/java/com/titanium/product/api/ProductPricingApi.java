package com.titanium.product.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.request.premium.PremiumQuoteRequest;
import com.titanium.product.api.response.premium.PremiumQuoteResponse;

import jakarta.validation.Valid;

/**
 * Product 定价能力远程契约。
 */
@FeignClient(name = "titanium-product-service", contextId = "productPricingApi", path = "/api/v1/products")
public interface ProductPricingApi {

    /**
     * 执行不落账的保费试算。
     */
    @PostMapping("/{productId}/premium-quotes")
    ApiResponse<PremiumQuoteResponse> quotePremium(
            @PathVariable("productId") String productId,
            @Valid @RequestBody PremiumQuoteRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId);
}
