package com.titanium.product.web.provider;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.ProductMaintenanceOfferingApi;
import com.titanium.product.api.response.maintenance.ProductMaintenanceOfferingResolutionResponse;
import com.titanium.product.application.query.maintenance.ProductMaintenanceOfferingQueryService;
import com.titanium.product.maintenance.aggregate.ProductMaintenanceOffering;

import lombok.RequiredArgsConstructor;

/** Product 保全 Offering 正式跨域 API 实现。 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Validated
public class ProductMaintenanceOfferingApiProvider implements ProductMaintenanceOfferingApi {

    private final ProductMaintenanceOfferingQueryService queryService;

    @Override
    public ApiResponse<ProductMaintenanceOfferingResolutionResponse> resolve(
            String productId,
            String productVersion,
            String planVersion,
            String policyStatus,
            String source,
            OffsetDateTime businessEffectiveAt,
            String tenantId) {
        ProductMaintenanceOffering offering = queryService.resolve(
                tenantId, productId, productVersion, planVersion, policyStatus, source,
                businessEffectiveAt.toLocalDateTime());
        return ApiResponse.success(new ProductMaintenanceOfferingResolutionResponse(
                offering.tenantId(), offering.productId(), offering.productVersion(), offering.planVersion(),
                offering.offeringId(), offering.offeringVersion(), offering.contentHash(),
                OffsetDateTime.now(ZoneOffset.UTC), offering.allowedItemCodes()));
    }
}
