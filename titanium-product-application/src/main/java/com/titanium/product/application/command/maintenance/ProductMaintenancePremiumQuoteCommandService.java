package com.titanium.product.application.command.maintenance;

import org.springframework.stereotype.Service;

import com.titanium.product.application.model.pricing.MaintenancePremiumQuoteResult;
import com.titanium.product.application.orchestration.pricing.maintenance.MaintenancePremiumQuoteApplicationService;
import com.titanium.product.command.maintenance.CreateMaintenancePremiumQuoteCommand;

import lombok.RequiredArgsConstructor;

/** Product 保全版本化报价写侧应用入口。 */
@Service
@RequiredArgsConstructor
public class ProductMaintenancePremiumQuoteCommandService {

    private final MaintenancePremiumQuoteApplicationService applicationService;

    public MaintenancePremiumQuoteResult quote(CreateMaintenancePremiumQuoteCommand command) {
        return applicationService.quote(command);
    }
}
