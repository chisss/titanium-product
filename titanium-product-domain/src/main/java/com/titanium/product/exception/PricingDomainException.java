package com.titanium.product.exception;

import java.io.Serial;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.exception.DomainException;

/**
 * 产品定价领域异常。
 */
public class PricingDomainException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public PricingDomainException(ProductErrorCode errorCode, String detail) {
        super(errorCode, errorCode.getMessage() + ": " + detail);
    }
}
