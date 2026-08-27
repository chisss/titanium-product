package com.titanium.product.application.query.pricing;

import java.util.List;

import org.springframework.stereotype.Service;

import com.titanium.product.aggregate.RateTableDefinition;
import com.titanium.product.application.orchestration.pricing.RateTableManagementApplicationService;
import com.titanium.product.common.enums.RateTableStatus;

import lombok.RequiredArgsConstructor;

/**
 * 费率表读侧应用入口。
 */
@Service
@RequiredArgsConstructor
public class RateTableQueryAppService {

    private final RateTableManagementApplicationService orchestrationService;

    public RateTableDefinition get(String tenantId, String productId, String tableId) {
        return orchestrationService.get(tenantId, productId, tableId);
    }

    public List<RateTableDefinition> list(String tenantId, String productId, RateTableStatus status) {
        return orchestrationService.list(tenantId, productId, status);
    }
}
