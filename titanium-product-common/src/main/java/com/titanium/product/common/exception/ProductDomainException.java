package com.titanium.product.common.exception;

/**
 * 产品域业务异常
 * 用于产品域业务规则校验失败时抛出
 */
public class ProductDomainException extends RuntimeException {

    private final String errorCode;

    public ProductDomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ProductDomainException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    // ====== 预定义异常工厂方法 ======

    public static ProductDomainException productNotFound(String productId) {
        return new ProductDomainException("PRODUCT_NOT_FOUND", "产品不存在: " + productId);
    }

    public static ProductDomainException invalidStatus(String expected, String actual) {
        return new ProductDomainException("INVALID_PRODUCT_STATUS",
                "产品状态不合法，期望: " + expected + "，实际: " + actual);
    }

    public static ProductDomainException duplicateProductCode(String productCode) {
        return new ProductDomainException("DUPLICATE_PRODUCT_CODE", "产品代码已存在: " + productCode);
    }

    public static ProductDomainException clauseBindingError(String message) {
        return new ProductDomainException("CLAUSE_BINDING_ERROR", "条款绑定错误: " + message);
    }

    public static ProductDomainException validationFailed(String message) {
        return new ProductDomainException("VALIDATION_FAILED", "校验失败: " + message);
    }
}
