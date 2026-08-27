package com.titanium.product.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.titanium.metadata.enums.pricing.AmountChannel;
import com.titanium.metadata.enums.pricing.ChargeCategory;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.enums.pricing.ChargePayerType;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.aggregate.CalculationModelDefinition;
import com.titanium.product.aggregate.ChargeComponentDefinition;
import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.common.enums.CalculationOperator;
import com.titanium.product.exception.PricingDomainException;
import com.titanium.product.valueobject.pricing.CalculationLine;
import com.titanium.product.valueobject.pricing.CalculationModelExecutionResult;
import com.titanium.product.valueobject.pricing.CalculationNode;
import com.titanium.product.valueobject.pricing.CalculationTotals;
import com.titanium.product.valueobject.pricing.PricingRoundingRule;

import lombok.RequiredArgsConstructor;

/**
 * 执行 V2-A 受约束计算 DAG 的纯领域服务。
 */
@Service
@RequiredArgsConstructor
public class CalculationModelExecutionService {

    private final CalculationTotalsService totalsService;

    public CalculationModelExecutionResult execute(
            CalculationModelDefinition model,
            List<ChargeComponentDefinition> components,
            BigDecimal standardPremium,
            PricingRoundingRule roundingRule,
            LocalDateTime businessTime) {
        if (model == null || model.getStatus() != ActuarialDefinitionStatus.PUBLISHED
                || !model.isEffectiveAt(businessTime) || standardPremium == null || standardPremium.signum() < 0
                || roundingRule == null) {
            throw invalid("计算模型、标准保费、业务时点或舍入规则不合法");
        }
        Map<String, ChargeComponentDefinition> componentByRef = indexComponents(model, components, businessTime);
        Map<String, BigDecimal> nodeOutputs = new LinkedHashMap<>();
        List<CalculationLine> lines = new ArrayList<>();
        String outputNodeCode = null;
        BigDecimal declaredOutput = null;
        for (CalculationNode node : model.topologicalNodes()) {
            List<BigDecimal> inputs = model.predecessors(node.nodeCode()).stream()
                    .map(nodeOutputs::get)
                    .toList();
            BigDecimal unsigned = calculateUnsigned(node, inputs, standardPremium, roundingRule);
            if (node.hasComponent()) {
                ChargeComponentDefinition component = componentByRef.get(ref(node.componentCode(), node.componentVersion()));
                CalculationLine line = toLine(model, node, component, inputs, unsigned, roundingRule);
                lines.add(line);
                nodeOutputs.put(node.nodeCode(), line.signedAmount());
            } else {
                nodeOutputs.put(node.nodeCode(), unsigned);
                outputNodeCode = node.nodeCode();
                declaredOutput = unsigned;
            }
        }
        CalculationTotals totals = totalsService.summarize(lines);
        if (declaredOutput == null || declaredOutput.compareTo(totals.customerPayable()) != 0) {
            throw invalid("输出节点金额与客户应付总额不一致");
        }
        return new CalculationModelExecutionResult(lines, totals, outputNodeCode);
    }

    public CalculationModelExecutionResult legacy(
            BigDecimal standardPremium, String currency, PricingRoundingRule roundingRule) {
        BigDecimal amount = round(standardPremium, roundingRule);
        CalculationLine line = new CalculationLine(
                "LEGACY_BASE_PREMIUM", "LEGACY_BASE_PREMIUM", "V1",
                ChargeCategory.RISK_PREMIUM, AmountChannel.CUSTOMER_PRICE, ChargeDirection.DEBIT,
                ChargePayerType.POLICYHOLDER, "PREMIUM", currency.toUpperCase(Locale.ROOT),
                amount, null, amount, "LEGACY_BASE_PREMIUM", true, "兼容基础保费");
        return new CalculationModelExecutionResult(
                List.of(line), CalculationTotals.customerPremium(amount), "LEGACY_CUSTOMER_PAYABLE");
    }

    private Map<String, ChargeComponentDefinition> indexComponents(
            CalculationModelDefinition model,
            List<ChargeComponentDefinition> components,
            LocalDateTime businessTime) {
        Map<String, ChargeComponentDefinition> result = new HashMap<>();
        for (ChargeComponentDefinition component : components == null
                ? List.<ChargeComponentDefinition>of()
                : components) {
            if (!model.getTenantId().equals(component.getTenantId())
                    || !model.getProductId().equals(component.getProductId())
                    || !component.isEffectiveAt(businessTime)) {
                continue;
            }
            result.put(ref(component.getComponentCode(), component.getComponentVersion()), component);
        }
        for (CalculationNode node : model.getNodes()) {
            if (node.hasComponent() && !result.containsKey(ref(node.componentCode(), node.componentVersion()))) {
                throw invalid("计算模型引用未发布或未生效的费用项: " + node.componentCode());
            }
        }
        return result;
    }

    private BigDecimal calculateUnsigned(
            CalculationNode node,
            List<BigDecimal> inputs,
            BigDecimal standardPremium,
            PricingRoundingRule roundingRule) {
        BigDecimal inputTotal = inputs.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal value = switch (node.operator()) {
            case STANDARD_PREMIUM -> standardPremium;
            case FIXED_AMOUNT -> node.parameterValue();
            case PERCENTAGE_OF -> {
                if (inputTotal.signum() < 0) {
                    throw invalid("比例节点计算基数不能为负数: " + node.nodeCode());
                }
                yield inputTotal.multiply(node.parameterValue());
            }
            case SUM -> inputTotal;
        };
        return round(value, roundingRule);
    }

    private CalculationLine toLine(
            CalculationModelDefinition model,
            CalculationNode node,
            ChargeComponentDefinition component,
            List<BigDecimal> inputs,
            BigDecimal amount,
            PricingRoundingRule roundingRule) {
        BigDecimal baseAmount = node.operator() == CalculationOperator.PERCENTAGE_OF
                ? round(inputs.stream().reduce(BigDecimal.ZERO, BigDecimal::add), roundingRule)
                : null;
        BigDecimal rate = node.operator() == CalculationOperator.PERCENTAGE_OF ? node.parameterValue() : null;
        return new CalculationLine(
                node.nodeCode(), component.getComponentCode(), component.getComponentVersion(),
                component.getCategory(), component.getAmountChannel(), component.getDirection(),
                component.getPayerType(), component.getAccountingClass(), model.getCurrency(),
                baseAmount, rate, amount.abs(), node.nodeCode(), component.isCustomerVisible(),
                component.getComponentName(), component.getAmountChannel() == AmountChannel.CUSTOMER_PRICE,
                null, null);
    }

    private BigDecimal round(BigDecimal amount, PricingRoundingRule roundingRule) {
        if (amount == null) {
            throw invalid("计算节点未产生金额");
        }
        return amount.setScale(roundingRule.scale(), roundingRule.roundingMode());
    }

    private String ref(String code, String version) {
        return code.toUpperCase(Locale.ROOT) + ':' + version;
    }

    private PricingDomainException invalid(String detail) {
        return new PricingDomainException(ProductErrorCode.ACTUARIAL_MODEL_VALIDATION_FAILED, detail);
    }
}
