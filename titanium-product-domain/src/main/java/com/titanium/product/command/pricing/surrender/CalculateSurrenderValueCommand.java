package com.titanium.product.command.pricing.surrender;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 确认退保价值并生成生命周期差额的应用命令。 */
public record CalculateSurrenderValueCommand(
        String tenantId,
        String surrenderRequestId,
        String bizNo,
        String originalBizNo,
        String originalCalculationId,
        LocalDate policyEffectiveDate,
        LocalDate surrenderDate,
        Integer policyYear,
        LocalDateTime businessTime,
        String reason) {
}
