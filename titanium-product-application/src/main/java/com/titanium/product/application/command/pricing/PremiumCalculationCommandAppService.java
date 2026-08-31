package com.titanium.product.application.command.pricing;

import org.springframework.stereotype.Service;

import com.titanium.product.aggregate.PremiumCalculation;
import com.titanium.product.application.orchestration.pricing.PremiumCalculationApplicationService;
import com.titanium.product.command.pricing.PremiumCalculationCommand;

import lombok.RequiredArgsConstructor;

/**
 * Product 确认计算写侧应用入口。
 */
@Service
@RequiredArgsConstructor
public class PremiumCalculationCommandAppService {

    private final PremiumCalculationApplicationService applicationService;

    public PremiumCalculation confirm(PremiumCalculationCommand command) {
        return applicationService.confirm(command);
    }
}
