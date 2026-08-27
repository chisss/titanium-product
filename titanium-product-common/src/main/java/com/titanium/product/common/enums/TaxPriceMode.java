package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * 税费在客户价格中的计入方式。
 */
@Getter
public enum TaxPriceMode implements BaseEnum {
    EXCLUSIVE(1, "EXCLUSIVE", "价外税"),
    INCLUSIVE(2, "INCLUSIVE", "价内税");

    private final Integer enumCode;
    private final String code;
    private final String name;

    TaxPriceMode(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    public static TaxPriceMode fromCode(String code) {
        return BaseEnum.fromCode(TaxPriceMode.class, code);
    }
}
