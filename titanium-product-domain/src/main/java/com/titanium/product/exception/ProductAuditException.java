package com.titanium.product.exception;

import java.io.Serial;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.exception.DomainException;

/**
 * 产品审核异常
 * <p>
 * 当产品审核流程中出现非法操作时抛出，如审核命令与审核结果不匹配（不通过却走通过命令）。 异常携带产品ID与具体原因，便于排查。
 */
public class ProductAuditException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 以错误码枚举构造产品审核异常。
     *
     * @param errorCode 产品域错误码（60 段）
     * @param message 异常消息
     */
    public ProductAuditException(ProductErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 构造产品审核异常。
     *
     * @param productId 产品ID
     * @param reason 异常原因
     */
    public ProductAuditException(String productId, String reason) {
        this(ProductErrorCode.PRODUCT_AUDIT_FAILED, String.format("产品[%s]审核异常: %s", productId, reason));
    }
}
