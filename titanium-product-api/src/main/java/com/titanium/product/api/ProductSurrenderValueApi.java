package com.titanium.product.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.request.SurrenderValueCalculationRequest;
import com.titanium.product.api.response.SurrenderValueCalculationResponse;

import jakarta.validation.Valid;

/** Product 退保价值确认远程契约。 */
@FeignClient(name = "titanium-product-service", contextId = "productSurrenderValueApi", path = "/api/v1")
public interface ProductSurrenderValueApi {

    @PostMapping("/premium-calculations/surrender-values")
    ApiResponse<SurrenderValueCalculationResponse> calculate(
            @Valid @RequestBody SurrenderValueCalculationRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId);
}
