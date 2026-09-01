package com.titanium.product.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.product.valueobject.pricing.calculation.CalculationLine;
import com.titanium.product.valueobject.pricing.calculation.CalculationModelExecutionResult;
import com.titanium.product.valueobject.pricing.commission.CommissionLineEvidence;
import com.titanium.product.valueobject.pricing.commission.CommissionResolutionInstruction;
import com.titanium.product.valueobject.pricing.commission.CommissionResolutionResult;

import lombok.RequiredArgsConstructor;

/**
 * 将 Channel 的确定性佣金结果合成为 Product 内部成本明细。
 */
@Service
@RequiredArgsConstructor
public class CommissionCalculationService {

    private final CalculationTotalsService totalsService;

    public CalculationModelExecutionResult append(
            CalculationModelExecutionResult breakdown,
            CommissionResolutionResult result) {
        List<CalculationLine> lines = new ArrayList<>(breakdown.lines());
        for (CommissionResolutionInstruction instruction : result.instructions()) {
            lines.add(toLine(result, instruction));
        }
        return new CalculationModelExecutionResult(
                lines, totalsService.summarize(lines), breakdown.outputNodeCode());
    }

    private CalculationLine toLine(
            CommissionResolutionResult result,
            CommissionResolutionInstruction instruction) {
        String evidenceKey = String.join("|", result.schemeCode(), result.schemeVersion(),
                instruction.beneficiaryType(), instruction.beneficiaryId());
        String lineId = UUID.nameUUIDFromBytes(evidenceKey.getBytes(StandardCharsets.UTF_8)).toString();
        CommissionLineEvidence evidence = new CommissionLineEvidence(
                result.channelId(), result.schemeCode(), result.schemeVersion(), result.schemeHash(),
                instruction.beneficiaryType(), instruction.beneficiaryId(), instruction.splitRate(),
                result.grossCommission(), instruction.installmentCount(), instruction.clawbackMonths());
        return new CalculationLine(
                lineId, result.schemeCode(), result.schemeVersion(), ChargeCategory.COMMISSION,
                AmountChannel.INTERNAL_COST, ChargeDirection.DEBIT, ChargePayerType.INSURER,
                "COMMISSION_PAYABLE", result.currency(), result.baseAmount(), instruction.splitRate(),
                instruction.amount(), "COMMISSION", false,
                "预计渠道佣金-" + instruction.beneficiaryType(), false, null, evidence);
    }
}
