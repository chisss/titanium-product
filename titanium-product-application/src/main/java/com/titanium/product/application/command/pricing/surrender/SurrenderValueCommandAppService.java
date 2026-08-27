package com.titanium.product.application.command.pricing.surrender;

import org.springframework.stereotype.Service;

import com.titanium.product.application.model.pricing.surrender.SurrenderValueCalculationResult;
import com.titanium.product.application.orchestration.pricing.surrender.SurrenderValueApplicationService;

import lombok.RequiredArgsConstructor;

/** Product 退保价值确认写侧应用入口。 */
@Service
@RequiredArgsConstructor
public class SurrenderValueCommandAppService {

    private final SurrenderValueApplicationService applicationService;

    public SurrenderValueCalculationResult calculate(CalculateSurrenderValueCommand command) {
        return applicationService.calculate(command);
    }
}
