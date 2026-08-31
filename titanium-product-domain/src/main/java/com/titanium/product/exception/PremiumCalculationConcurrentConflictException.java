package com.titanium.product.exception;

import java.io.Serial;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.exception.DomainException;

/**
 * 确认计算唯一键被并发请求抢先写入。
 * <p>
 * 调用方捕获后按幂等语义重试查询即可，无需回滚业务。
 * </p>
 */
public class PremiumCalculationConcurrentConflictException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public PremiumCalculationConcurrentConflictException(Throwable cause) {
        super(ProductErrorCode.CALCULATION_CONCURRENT_CONFLICT,
                ProductErrorCode.CALCULATION_CONCURRENT_CONFLICT.getMessage(), cause);
    }
}
