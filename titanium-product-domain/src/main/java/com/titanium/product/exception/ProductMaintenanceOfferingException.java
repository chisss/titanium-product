package com.titanium.product.exception;

import java.io.Serial;

import com.titanium.metadata.exception.DomainException;
import com.titanium.product.common.enums.ProductMaintenanceOfferingFailureReason;
import com.titanium.product.common.errorcode.ProductMaintenanceOfferingErrorCode;

import lombok.Getter;

/** Product 保全 Offering 配置或适用性异常。 */
@Getter
public class ProductMaintenanceOfferingException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ProductMaintenanceOfferingFailureReason reason;
    private final ProductMaintenanceOfferingErrorCode offeringErrorCode;

    public ProductMaintenanceOfferingException(
            ProductMaintenanceOfferingFailureReason reason,
            String message) {
        super(errorCode(reason).getCode(), message);
        this.reason = reason;
        this.offeringErrorCode = errorCode(reason);
    }

    private static ProductMaintenanceOfferingErrorCode errorCode(
            ProductMaintenanceOfferingFailureReason reason) {
        return switch (requireReason(reason)) {
            case NOT_FOUND -> ProductMaintenanceOfferingErrorCode.NOT_FOUND;
            case ALREADY_EXISTS -> ProductMaintenanceOfferingErrorCode.ALREADY_EXISTS;
            case VERSION_MISMATCH -> ProductMaintenanceOfferingErrorCode.VERSION_MISMATCH;
            case NOT_APPLICABLE -> ProductMaintenanceOfferingErrorCode.NOT_APPLICABLE;
            case PERIOD_CONFLICT -> ProductMaintenanceOfferingErrorCode.PERIOD_CONFLICT;
            case STATE_INVALID -> ProductMaintenanceOfferingErrorCode.STATUS_INVALID;
            case CONTRACT_INVALID -> ProductMaintenanceOfferingErrorCode.CONTRACT_INVALID;
        };
    }

    private static ProductMaintenanceOfferingFailureReason requireReason(
            ProductMaintenanceOfferingFailureReason reason) {
        if (reason == null) {
            throw new IllegalArgumentException("Product保全Offering失败原因不能为空");
        }
        return reason;
    }
}
