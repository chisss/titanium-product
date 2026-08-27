package com.titanium.product.application.command.pricing;

import org.springframework.stereotype.Service;

import com.titanium.product.application.orchestration.pricing.RateTableManagementApplicationService;
import com.titanium.product.valueobject.RateTableValidationResult;

import lombok.RequiredArgsConstructor;

/**
 * 费率表写侧应用入口。
 */
@Service
@RequiredArgsConstructor
public class RateTableCommandAppService {

    private final RateTableManagementApplicationService orchestrationService;

    public String createDraft(CreateRateTableDraftCommand command) {
        return orchestrationService.createDraft(command);
    }

    public void replaceRows(ReplaceRateTableRowsCommand command) {
        orchestrationService.replaceRows(command);
    }

    public RateTableValidationResult validate(String tenantId, String productId, String tableId) {
        return orchestrationService.validate(tenantId, productId, tableId);
    }

    public RateTableValidationResult publish(String tenantId, String productId, String tableId) {
        return orchestrationService.publish(tenantId, productId, tableId);
    }

    public void retire(String tenantId, String productId, String tableId) {
        orchestrationService.retire(tenantId, productId, tableId);
    }
}
