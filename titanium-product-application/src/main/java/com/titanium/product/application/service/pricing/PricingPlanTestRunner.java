package com.titanium.product.application.service.pricing;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.exception.DomainException;
import com.titanium.product.application.orchestration.pricing.PricingCalculationInput;
import com.titanium.product.application.orchestration.pricing.PricingCalculationOutcome;
import com.titanium.product.application.orchestration.pricing.PricingPlanCalculator;
import com.titanium.product.common.constant.ProductConstants;
import com.titanium.product.pricing.aggregate.PricingPlanDefinition;
import com.titanium.product.valueobject.pricing.pricing.PricingPlanValidationResult;
import com.titanium.product.valueobject.pricing.pricing.PricingTestCase;
import com.titanium.product.valueobject.pricing.pricing.PricingTestCaseResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 定价方案发布回归运行器。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PricingPlanTestRunner {

    private final PricingPlanCalculator pricingPlanCalculator;

    /** 执行方案当前版本的全部发布回归用例。 */
    public PricingPlanValidationResult run(PricingPlanDefinition plan) {
        List<PricingTestCaseResult> results = plan.testCases().stream()
                .map(testCase -> runCase(plan, testCase))
                .toList();
        int passed = (int) results.stream().filter(PricingTestCaseResult::passed).count();
        return new PricingPlanValidationResult(plan.contentHash(), results.size(), passed, results);
    }

    private PricingTestCaseResult runCase(PricingPlanDefinition plan, PricingTestCase testCase) {
        try {
            PricingCalculationOutcome calculation = pricingPlanCalculator.calculateForValidation(
                    plan, toInput(plan, testCase));
            BigDecimal actualPremium = calculation.totalPremium();
            BigDecimal difference = actualPremium.subtract(testCase.expectedPremium()).abs();
            boolean passed = difference.compareTo(testCase.tolerance()) <= 0;
            return new PricingTestCaseResult(
                    testCase.caseCode(), passed, testCase.expectedPremium(), actualPremium, difference,
                    passed ? null : ProductConstants.PRICING_TEST_CASE_TOLERANCE_EXCEEDED);
        } catch (RuntimeException exception) {
            String failureReason = failureMessage(exception);
            // 异常可被吞（用于逐条收集结果），但不可不留痕：服务端须能按 planId/caseCode 反查失败原因（D-501-36）
            log.warn("定价回归用例计算失败: planId={}, caseCode={}, errorCode={}",
                    plan.planId(), testCase.caseCode(), failureReason, exception);
            return new PricingTestCaseResult(
                    testCase.caseCode(), false, testCase.expectedPremium(), null, null, failureReason);
        }
    }

    private PricingCalculationInput toInput(PricingPlanDefinition plan, PricingTestCase testCase) {
        return new PricingCalculationInput(
                plan.tenantId(), plan.productId(), executionId(plan, testCase), testCase.businessTime(),
                plan.currency(), testCase.sumInsured(), testCase.age(), testCase.gender(),
                testCase.paymentTermYears(), testCase.coverageTermYears(), testCase.paymentPeriods(),
                testCase.requestSnapshot(), channelId(testCase), policyYear(testCase));
    }

    private String channelId(PricingTestCase testCase) {
        Object value = testCase.requestSnapshot().get("channelId");
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value).trim();
    }

    private int policyYear(PricingTestCase testCase) {
        Object value = testCase.requestSnapshot().get("policyYear");
        if (value instanceof Number number) {
            return Math.max(number.intValue(), 1);
        }
        if (value instanceof String text) {
            try {
                return Math.max(Integer.parseInt(text.trim()), 1);
            } catch (NumberFormatException ignored) {
                // 非法快照值按首个保单年度执行，保持旧用例兼容。
            }
        }
        return 1;
    }

    private String executionId(PricingPlanDefinition plan, PricingTestCase testCase) {
        return plan.planId() + ':' + testCase.caseCode();
    }

    /**
     * 用例执行异常 → 稳定失败标识（红线20：异常原始消息不得进入结果载荷，仅保留稳定错误码或异常类名供排查）。
     * <p>
     * 🔴 {@link DomainException}（全平台领域异常基类，携带 8 位业务码）与 {@link BusinessException} 无继承关系，
     * 须<b>并列</b>判定——本域抛的是 {@code PricingDomainException extends DomainException}，此前只判后者，
     * 致失败原因被降级为异常类名（错误码与文案双丢失，D-501-36）。
     * </p>
     */
    private String failureMessage(RuntimeException exception) {
        if (exception instanceof DomainException domain && domain.getErrorCode() != null) {
            return domain.getErrorCode();
        }
        if (exception instanceof BusinessException business && business.getErrorCode() != null) {
            return business.getErrorCode();
        }
        return exception.getClass().getSimpleName();
    }
}
