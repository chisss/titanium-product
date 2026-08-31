package com.titanium.product.exception;

import java.io.Serial;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.exception.DomainException;

/**
 * 产品状态前置条件不满足异常
 * <p>
 * 当对产品聚合根执行某项操作时，其当前状态不满足该操作所要求的前置状态时抛出。
 * 如：仅草稿状态可提交审核、仅生效产品可下架等。异常携带产品ID、当前状态与操作名称， 便于前端国际化与日志检索。
 */
public class ProductStatusPreconditionException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 以错误码枚举构造产品状态前置条件异常。
     *
     * @param errorCode 产品域错误码（60 段）
     * @param message 异常消息
     */
    public ProductStatusPreconditionException(ProductErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 构造产品状态前置条件异常。
     *
     * @param productId 产品ID
     * @param currentStatus 当前状态
     * @param operation 正在执行的操作名称
     */
    public ProductStatusPreconditionException(String productId, String currentStatus, String operation) {
        this(ProductErrorCode.PRODUCT_STATUS_PRECONDITION_FAILED,
                String.format("产品[%s]当前状态[%s]不满足操作[%s]的前置条件", productId, currentStatus, operation));
    }
}
