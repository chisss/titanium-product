package com.titanium.product.api;

import java.time.OffsetDateTime;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.response.ProductMaintenanceOfferingResolutionResponse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Product 保全 Offering 正式跨域契约。 */
@FeignClient(
        name = "titanium-product-service",
        contextId = "productMaintenanceOfferingApi",
        path = "/api/v1/products")
public interface ProductMaintenanceOfferingApi {

    /** 按产品、计划和业务上下文解析可选保全项。 */
    @GetMapping("/{productId}/maintenance-offering")
    ApiResponse<ProductMaintenanceOfferingResolutionResponse> resolve(
            @PathVariable("productId") @NotBlank @Size(max = 36) String productId,
            @RequestParam("productVersion") @NotBlank @Size(max = 64) String productVersion,
            @RequestParam("planVersion") @NotBlank @Size(max = 64) String planVersion,
            @RequestParam("policyStatus") @NotBlank @Size(max = 32) String policyStatus,
            @RequestParam("source") @NotBlank @Size(max = 32) String source,
            @RequestParam("businessEffectiveAt")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @NotNull OffsetDateTime businessEffectiveAt,
            @RequestHeader("X-Tenant-Id") @NotBlank @Size(max = 64) String tenantId);
}
