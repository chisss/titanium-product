package com.titanium.product.application.command.pricing.lifecycle;

import org.springframework.stereotype.Service;

import com.titanium.product.application.model.RetroactivePremiumPeriodRecalculationResult;
import com.titanium.product.application.orchestration.pricing.lifecycle.RetroactivePremiumPeriodRecalculationApplicationService;
import com.titanium.product.command.pricing.lifecycle.RecalculateRetroactivePremiumPeriodsCommand;

import lombok.RequiredArgsConstructor;

/**
 * 追溯期间保费重算写侧应用入口。
 */
@Service
@RequiredArgsConstructor
public class RetroactivePremiumPeriodRecalculationCommandAppService {

    private final RetroactivePremiumPeriodRecalculationApplicationService applicationService;

    public RetroactivePremiumPeriodRecalculationResult recalculate(
            RecalculateRetroactivePremiumPeriodsCommand command) {
        return applicationService.recalculate(command);
    }
}
