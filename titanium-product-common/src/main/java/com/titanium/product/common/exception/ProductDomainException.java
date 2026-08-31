package com.titanium.product.common.exception;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.exception.DomainException;

/**
 * 产品域业务异常
 * 用于产品域业务规则校验失败时抛出
 */
public class ProductDomainException extends DomainException {

    /**
     * 以错误码枚举构造产品域业务异常。
     *
     * @param errorCode 产品域错误码（60 段）
     * @param message 异常消息
     */
    public ProductDomainException(ProductErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 以错误码枚举构造产品域业务异常（含根因）。
     *
     * @param errorCode 产品域错误码（60 段）
     * @param message 异常消息
     * @param cause 根因异常
     */
    public ProductDomainException(ProductErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 以裸串错误码构造异常（旧用法）。
     *
     * @param errorCode 错误码字符串
     * @param message 异常消息
     * @deprecated 裸串错误码违反红线 19，请改用 {@link #ProductDomainException(ProductErrorCode, String)}
     */
    @Deprecated
    public ProductDomainException(String errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 以裸串错误码构造异常（旧用法，含根因）。
     *
     * @param errorCode 错误码字符串
     * @param message 异常消息
     * @param cause 根因异常
     * @deprecated 裸串错误码违反红线 19，请改用 {@link #ProductDomainException(ProductErrorCode, String, Throwable)}
     */
    @Deprecated
    public ProductDomainException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    // ====== 预定义异常工厂方法 ======

    public static ProductDomainException productNotFound(String productId) {
        return new ProductDomainException(ProductErrorCode.PRODUCT_NOT_EXIST, "产品不存在: " + productId);
    }

    public static ProductDomainException invalidStatus(String expected, String actual) {
        return new ProductDomainException(ProductErrorCode.PRODUCT_STATUS_ERROR,
                "产品状态不合法，期望: " + expected + "，实际: " + actual);
    }

    public static ProductDomainException duplicateProductCode(String productCode) {
        return new ProductDomainException(ProductErrorCode.PRODUCT_ALREADY_EXIST, "产品代码已存在: " + productCode);
    }

    public static ProductDomainException clauseBindingError(String message) {
        return new ProductDomainException(ProductErrorCode.CLAUSE_BINDING_FAILED, "条款绑定错误: " + message);
    }

    public static ProductDomainException validationFailed(String message) {
        return new ProductDomainException(ProductErrorCode.PRODUCT_VALIDATION_FAILED, "校验失败: " + message);
    }
}
