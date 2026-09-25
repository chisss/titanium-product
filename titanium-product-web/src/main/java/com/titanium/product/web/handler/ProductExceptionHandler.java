package com.titanium.product.web.handler;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.titanium.common.exception.BusinessException;
import com.titanium.metadata.errorcode.BaseErrorCode;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.errorcode.SystemErrorCode;
import com.titanium.metadata.exception.DomainException;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.common.enums.ProductMaintenanceOfferingFailureReason;
import com.titanium.product.exception.ProductMaintenanceOfferingException;

/**
 * Product Web 统一异常处理器。
 */
@RestControllerAdvice(basePackages = "com.titanium.product.web")
public class ProductExceptionHandler {

    /**
     * 处理应用层业务异常。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return productError(exception.getErrorCodeValue(), exception.getMessage())
                .orElseGet(() -> systemError(exception.getErrorCodeValue(), exception.getMessage()));
    }

    /** 处理 Product 保全 Offering 的稳定跨域错误语义。 */
    @ExceptionHandler(ProductMaintenanceOfferingException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaintenanceOfferingException(
            ProductMaintenanceOfferingException exception) {
        return ResponseEntity.status(statusFor(exception.getReason()))
                .body(ApiResponse.error(exception.getOfferingErrorCode(), exception.getMessage()));
    }

    /**
     * 处理领域规则异常。
     * <p>
     * 错误码字段自 2026-09-25 起为 {@link BaseErrorCode} 强类型：携 {@link ProductErrorCode} 者
     * 直接按其语义定状态码（原有「按码串反查枚举」的结果等价，但不再丢失枚举身份）；
     * 其余（存量裸串路径、系统通用码）维持原行为——按码串回查本域枚举，未命中则 422 + 参数无效。
     * </p>
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(DomainException exception) {
        BaseErrorCode errorCode = exception.getErrorCode();
        if (errorCode instanceof ProductErrorCode productErrorCode) {
            return ResponseEntity.status(statusFor(productErrorCode))
                    .body(ApiResponse.error(productErrorCode, exception.getMessage()));
        }
        return productError(errorCode.getCode(), exception.getMessage())
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(ApiResponse.error(SystemErrorCode.PARAM_INVALID, exception.getMessage())));
    }

    /**
     * 处理 HTTP 请求参数校验错误。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse(ProductErrorCode.PRICING_INPUT_INVALID.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ProductErrorCode.PRICING_INPUT_INVALID, message));
    }

    /**
     * 按码串反查本域枚举（仅服务于 {@link BusinessException} 分支与存量裸串路径——
     * 二者的错误码都只是字符串，无枚举身份可直取）。
     */
    private Optional<ResponseEntity<ApiResponse<Void>>> productError(String code, String message) {
        return Arrays.stream(ProductErrorCode.values())
                .filter(errorCode -> errorCode.getCode().equals(code))
                .findFirst()
                .map(errorCode -> ResponseEntity.status(statusFor(errorCode))
                        .body(ApiResponse.error(errorCode, message)));
    }

    private ResponseEntity<ApiResponse<Void>> systemError(String code, String message) {
        return Arrays.stream(SystemErrorCode.values())
                .filter(errorCode -> errorCode.getCode().equals(code))
                .findFirst()
                .map(errorCode -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.<Void>error(errorCode, message)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error(SystemErrorCode.SYSTEM_ERROR, message)));
    }

    private HttpStatus statusFor(ProductErrorCode errorCode) {
        return switch (errorCode) {
            case PRODUCT_NOT_EXIST, PRODUCT_TEMPLATE_NOT_EXIST, RATE_TABLE_NOT_FOUND,
                    PRICING_PLAN_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case RATE_ROW_MULTIPLE_MATCHED, RATE_TABLE_ALREADY_EXISTS, RATE_TABLE_STATUS_INVALID,
                    RATE_TABLE_ROW_CONFLICT, PRICING_PLAN_ALREADY_EXISTS, PRICING_PLAN_STATUS_INVALID,
                    PRICING_PLAN_EFFECTIVE_PERIOD_CONFLICT -> HttpStatus.CONFLICT;
            case PRICING_INPUT_INVALID -> HttpStatus.BAD_REQUEST;
            case PRICING_PLAN_NOT_EFFECTIVE, RATE_TABLE_NOT_EFFECTIVE, RATE_ROW_NOT_MATCHED,
                    PRICING_CURRENCY_MISMATCH, PRICING_MODE_UNSUPPORTED,
                    RATE_TABLE_VALIDATION_FAILED, PRICING_PLAN_VALIDATION_FAILED,
                    PRICING_TEST_CASE_FAILED, PRICING_ADJUSTMENT_INVALID -> HttpStatus.UNPROCESSABLE_ENTITY;
            case PRICING_IDEMPOTENCY_CONFLICT -> HttpStatus.CONFLICT;
            case PRICING_CALCULATION_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case PRICING_DEPENDENCY_FAILED, PRICING_DEPENDENCY_RESPONSE_INVALID -> HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private HttpStatus statusFor(ProductMaintenanceOfferingFailureReason reason) {
        return switch (reason) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ALREADY_EXISTS, PERIOD_CONFLICT, STATE_INVALID -> HttpStatus.CONFLICT;
            case VERSION_MISMATCH, NOT_APPLICABLE, CONTRACT_INVALID -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
    }
}
