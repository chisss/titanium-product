package com.titanium.product.exception;

import java.io.Serial;

/**
 * 生命周期差额事实唯一键被并发请求抢先写入。
 */
public class PremiumLifecycleAdjustmentConcurrentConflictException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public PremiumLifecycleAdjustmentConcurrentConflictException(Throwable cause) {
        super(cause);
    }
}
