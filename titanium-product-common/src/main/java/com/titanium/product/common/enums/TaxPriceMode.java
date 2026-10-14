package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;
import com.titanium.metadata.enums.Reserved;

import lombok.Getter;

/**
 * 税费在客户价格中的计入方式。
 */
@Getter
public enum TaxPriceMode implements BaseEnum {
    EXCLUSIVE(1, "EXCLUSIVE", "价外税"),
    @Reserved("2026-10-14 g16-05 零引用登记（枚举级消费）：存在 values/valueOf/自定方法/:: 调用，常量名零出现；接入时摘标注+删清单行。见 docs/技术文档/枚举零引用清单.md")
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
