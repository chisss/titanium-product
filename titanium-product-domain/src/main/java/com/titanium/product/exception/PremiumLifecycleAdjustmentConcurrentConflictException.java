package com.titanium.product.exception;

import java.io.Serial;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.exception.DomainException;

/**
 * 生命周期差额事实唯一键被并发请求抢先写入。
 * <p>
 * 调用方捕获后按幂等语义重试查询即可，无需回滚业务。
 * </p>
 */
public class PremiumLifecycleAdjustmentConcurrentConflictException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public PremiumLifecycleAdjustmentConcurrentConflictException(Throwable cause) {
        super(ProductErrorCode.LIFECYCLE_ADJUSTMENT_CONCURRENT_CONFLICT,
                ProductErrorCode.LIFECYCLE_ADJUSTMENT_CONCURRENT_CONFLICT.getMessage(), cause);
    }
}
