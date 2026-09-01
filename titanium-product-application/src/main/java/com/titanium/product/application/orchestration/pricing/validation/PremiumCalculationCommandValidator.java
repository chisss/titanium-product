package com.titanium.product.application.orchestration.pricing.validation;

import java.util.List;

import org.springframework.stereotype.Component;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.command.pricing.PremiumCalculationCommand;
import com.titanium.product.common.enums.PricingCalculationPurpose;
import com.titanium.product.valueobject.pricing.premium.PremiumAdjustmentRequest;

/**
 * 保费确认计算命令校验器。
 * <p>
 * 把原先散落在 {@code PremiumCalculationApplicationService#validateCommand} 的 16 处条件校验链
 * 收敛为独立校验类（规约红线 22：&gt;3 参数 if 校验链须抽象独立校验类），校验顺序与原实现一致，
 * 违反时统一抛出携带 {@link ProductErrorCode#PRICING_INPUT_INVALID} 错误码的业务异常（红线 19）。
 * 分组方法按「命令骨架 → 定价要素 → 标的信息 → 保障期限 → 核保调整」拆分，单方法单职责。
 * </p>
 */
@Component
public class PremiumCalculationCommandValidator {

    /** 校验确认计算命令的完整性与合法性（任一条件不满足即抛出 PRICING_INPUT_INVALID）。 */
    public void validate(PremiumCalculationCommand command) {
        if (command == null) {
            throw invalid();
        }
        validateSkeleton(command);
        validatePricingFactors(command);
        validateSubject(command);
        validateTerms(command);
        validateAdjustments(command);
    }

    /** 校验命令骨架：租户、产品、请求标识、业务号与计算用途。 */
    private void validateSkeleton(PremiumCalculationCommand command) {
        if (blank(command.tenantId()) || blank(command.productId())
                || blank(command.calculationRequestId()) || blank(command.bizNo())
                || unsupportedPurpose(command.purpose())) {
            throw invalid();
        }
    }

    /** 校验定价要素：产品版本、业务时点、币种与保额（保额须为正）。 */
    private void validatePricingFactors(PremiumCalculationCommand command) {
        if (blank(command.productVersion()) || command.businessTime() == null || blank(command.currency())
                || command.sumInsured() == null || command.sumInsured().signum() <= 0) {
            throw invalid();
        }
    }

    /** 校验标的信息：年龄须落在 0-120 且性别必填。 */
    private void validateSubject(PremiumCalculationCommand command) {
        if (command.age() < 0 || command.age() > 120 || blank(command.gender())) {
            throw invalid();
        }
    }

    /** 校验保障期限：缴费年限、保障年限与缴费期数均须为正。 */
    private void validateTerms(PremiumCalculationCommand command) {
        if (command.paymentTermYears() <= 0 || command.coverageTermYears() <= 0
                || command.paymentPeriods() <= 0) {
            throw invalid();
        }
    }

    /** 校验核保调整清单：条目非空、调整码/类型/值齐全且值非负。 */
    private void validateAdjustments(PremiumCalculationCommand command) {
        if (hasInvalidAdjustment(command.underwritingAdjustments())) {
            throw invalid();
        }
    }

    private boolean hasInvalidAdjustment(List<PremiumAdjustmentRequest> adjustments) {
        return adjustments.stream().anyMatch(adjustment -> adjustment == null
                || blank(adjustment.adjustmentCode()) || adjustment.type() == null
                || adjustment.value() == null || adjustment.value().signum() < 0);
    }

    private boolean unsupportedPurpose(PricingCalculationPurpose purpose) {
        return purpose != PricingCalculationPurpose.ISSUANCE_CONFIRM
                && purpose != PricingCalculationPurpose.MAINTENANCE;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    /** 统一入参非法异常（文案由错误码枚举承载，动态渲染在边界层完成）。 */
    private BusinessException invalid() {
        return new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
    }
}
