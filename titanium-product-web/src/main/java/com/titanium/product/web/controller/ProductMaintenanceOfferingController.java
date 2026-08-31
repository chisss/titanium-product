package com.titanium.product.web.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.application.command.maintenance.ProductMaintenanceOfferingCommandService;
import com.titanium.product.application.query.maintenance.ProductMaintenanceOfferingQueryService;
import com.titanium.product.command.maintenance.CreateProductMaintenanceOfferingCommand;
import com.titanium.product.maintenance.aggregate.ProductMaintenanceOffering;
import com.titanium.product.web.dto.maintenance.CreateProductMaintenanceOfferingDTO;
import com.titanium.product.web.dto.maintenance.ProductMaintenanceOfferingVO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/** Product 保全 Offering 后台管理接口。 */
@Validated
@RestController
@RequestMapping("/web/v1/products/{productId}/maintenance-offerings")
@RequiredArgsConstructor
public class ProductMaintenanceOfferingController {

    private final ProductMaintenanceOfferingCommandService commandService;
    private final ProductMaintenanceOfferingQueryService queryService;

    @PostMapping
    public ApiResponse<String> createDraft(
            @PathVariable @NotBlank @Size(max = 36) String productId,
            @RequestHeader("X-Tenant-Id") @NotBlank @Size(max = 64) String tenantId,
            @Valid @RequestBody CreateProductMaintenanceOfferingDTO request) {
        return ApiResponse.success(commandService.createDraft(new CreateProductMaintenanceOfferingCommand(
                tenantId, productId, request.productVersion(), request.planVersion(), request.offeringVersion(),
                request.effectiveFrom(), request.effectiveTo(), request.allowedPolicyStatuses(),
                request.allowedChannels(), request.allowedItemCodes())));
    }

    @PostMapping("/{offeringId}/publish")
    public ApiResponse<ProductMaintenanceOfferingVO> publish(
            @PathVariable @NotBlank @Size(max = 36) String productId,
            @PathVariable @NotBlank @Size(max = 36) String offeringId,
            @RequestHeader("X-Tenant-Id") @NotBlank @Size(max = 64) String tenantId) {
        return ApiResponse.success(toResponse(commandService.publish(tenantId, productId, offeringId)));
    }

    @PostMapping("/{offeringId}/retire")
    public ApiResponse<ProductMaintenanceOfferingVO> retire(
            @PathVariable @NotBlank @Size(max = 36) String productId,
            @PathVariable @NotBlank @Size(max = 36) String offeringId,
            @RequestHeader("X-Tenant-Id") @NotBlank @Size(max = 64) String tenantId) {
        return ApiResponse.success(toResponse(commandService.retire(tenantId, productId, offeringId)));
    }

    @GetMapping("/{offeringId}")
    public ApiResponse<ProductMaintenanceOfferingVO> get(
            @PathVariable @NotBlank @Size(max = 36) String productId,
            @PathVariable @NotBlank @Size(max = 36) String offeringId,
            @RequestHeader("X-Tenant-Id") @NotBlank @Size(max = 64) String tenantId) {
        return ApiResponse.success(toResponse(queryService.get(tenantId, productId, offeringId)));
    }

    private ProductMaintenanceOfferingVO toResponse(ProductMaintenanceOffering offering) {
        return new ProductMaintenanceOfferingVO(
                offering.offeringId(), offering.productId(), offering.productVersion(), offering.planVersion(),
                offering.offeringVersion(), offering.effectiveFrom(), offering.effectiveTo(),
                offering.status().getCode(), offering.contentHash(), offering.allowedPolicyStatuses(),
                offering.allowedChannels(), offering.allowedItemCodes());
    }
}
