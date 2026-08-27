package com.titanium.product.exception;

import java.io.Serial;

/**
 * 确认计算唯一键被并发请求抢先写入。
 */
public class PremiumCalculationConcurrentConflictException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public PremiumCalculationConcurrentConflictException(Throwable cause) {
        super(cause);
    }
}
