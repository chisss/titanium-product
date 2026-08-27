package com.titanium.product.application.query.maintenance;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.titanium.product.application.orchestration.maintenance.ProductMaintenanceOfferingManagementApplicationService;
import com.titanium.product.maintenance.aggregate.ProductMaintenanceOffering;

import lombok.RequiredArgsConstructor;

/** Product 保全 Offering 查询门面。 */
@Service
@RequiredArgsConstructor
public class ProductMaintenanceOfferingQueryService {

    private final ProductMaintenanceOfferingManagementApplicationService managementService;

    public ProductMaintenanceOffering get(String tenantId, String productId, String offeringId) {
        return managementService.get(tenantId, productId, offeringId);
    }

    public ProductMaintenanceOffering resolve(
            String tenantId,
            String productId,
            String productVersion,
            String planVersion,
            String policyStatus,
            String source,
            LocalDateTime businessTime) {
        return managementService.resolve(
                tenantId, productId, productVersion, planVersion, policyStatus, source, businessTime);
    }
}
