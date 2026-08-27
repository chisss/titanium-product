package com.titanium.product.application.command.pricing.lifecycle;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.titanium.product.application.model.RetroactivePremiumPeriodRecalculationResult;
import com.titanium.product.application.orchestration.pricing.lifecycle.RetroactivePremiumPeriodRecalculationApplicationService;

class RetroactivePremiumPeriodRecalculationCommandAppServiceTest {

    @Test
    void shouldDelegateRecalculationToOrchestrationService() {
        RetroactivePremiumPeriodRecalculationApplicationService applicationService =
                mock(RetroactivePremiumPeriodRecalculationApplicationService.class);
        RetroactivePremiumPeriodRecalculationCommandAppService commandAppService =
                new RetroactivePremiumPeriodRecalculationCommandAppService(applicationService);
        RecalculateRetroactivePremiumPeriodsCommand command = command();
        RetroactivePremiumPeriodRecalculationResult expected =
                mock(RetroactivePremiumPeriodRecalculationResult.class);
        when(applicationService.recalculate(command)).thenReturn(expected);

        RetroactivePremiumPeriodRecalculationResult actual = commandAppService.recalculate(command);

        assertSame(expected, actual);
        verify(applicationService).recalculate(command);
    }

    private RecalculateRetroactivePremiumPeriodsCommand command() {
        return new RecalculateRetroactivePremiumPeriodsCommand(
                "tenant-1", "request-1", "maintenance-1", "policy-1", "analysis-1", 1,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "calculation-original", "calculation-replacement",
                LocalDateTime.of(2026, 6, 1, 0, 0), LocalDateTime.of(2026, 8, 26, 0, 0), List.of());
    }
}
