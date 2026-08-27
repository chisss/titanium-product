package com.titanium.product.application.command.maintenance;

import org.springframework.stereotype.Service;

import com.titanium.product.application.orchestration.maintenance.ProductMaintenanceOfferingManagementApplicationService;
import com.titanium.product.maintenance.aggregate.ProductMaintenanceOffering;

import lombok.RequiredArgsConstructor;

/** Product 保全 Offering 命令门面。 */
@Service
@RequiredArgsConstructor
public class ProductMaintenanceOfferingCommandService {

    private final ProductMaintenanceOfferingManagementApplicationService managementService;

    public String createDraft(CreateProductMaintenanceOfferingCommand command) {
        return managementService.createDraft(command);
    }

    public ProductMaintenanceOffering publish(String tenantId, String productId, String offeringId) {
        return managementService.publish(tenantId, productId, offeringId);
    }

    public ProductMaintenanceOffering retire(String tenantId, String productId, String offeringId) {
        return managementService.retire(tenantId, productId, offeringId);
    }
}
