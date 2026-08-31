package com.titanium.product.application.service.pricing.validation;

import org.springframework.stereotype.Component;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.command.pricing.PremiumQuoteCommand;

/**
 * 保费试算命令校验器（红线22：>3 参数的校验逻辑抽象独立校验类）。
 * <p>
 * 校验试算命令骨架字段的完整性与值域合法性（租户/产品/请求号/业务时间/币种/保额/年龄/性别/
 * 缴费期/保障期/缴费频次），不合法时抛 {@link BusinessException}（携带 {@link ProductErrorCode}）。
 * </p>
 */
@Component
public class PremiumQuoteCommandValidator {

    /**
     * 校验保费试算命令，不合法时抛 PRICING_INPUT_INVALID。
     *
     * @param command 保费试算命令
     */
    public void validate(PremiumQuoteCommand command) {
        if (command == null || isBlank(command.tenantId()) || isBlank(command.productId())
                || isBlank(command.requestId()) || command.businessTime() == null || isBlank(command.currency())
                || command.sumInsured() == null || command.sumInsured().signum() <= 0
                || command.age() < 0 || command.age() > 120 || isBlank(command.gender())
                || command.paymentTermYears() <= 0 || command.coverageTermYears() <= 0
                || command.paymentPeriods() <= 0) {
            throw new BusinessException(ProductErrorCode.PRICING_INPUT_INVALID);
        }
    }

    /** 字符串判空（null 或空白）。 */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
