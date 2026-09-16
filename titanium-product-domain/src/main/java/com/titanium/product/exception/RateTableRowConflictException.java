package com.titanium.product.exception;

import java.io.Serial;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.exception.DomainException;

/**
 * 费率行配置冲突（同一维度组合重复，或区间可能同时命中）。
 * <p>
 * 领域守卫已在保存入口拦截维度重复（{@code RateTableDefinition.replaceRows}）；本类同时用于落库适配器
 * 兜底 —— DB 唯一约束 {@code uk_product_rate_row_dimension} 是最后一道防线，命中时不得以 HTTP 500 呈现
 * （D-501-34）。
 * </p>
 */
public class RateTableRowConflictException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RateTableRowConflictException(Throwable cause) {
        super(ProductErrorCode.RATE_TABLE_ROW_CONFLICT,
                ProductErrorCode.RATE_TABLE_ROW_CONFLICT.getMessage(), cause);
    }
}
