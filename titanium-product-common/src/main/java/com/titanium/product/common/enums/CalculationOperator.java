package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * V2-A 首批计算运算符。
 */
@Getter
public enum CalculationOperator implements BaseEnum {
    STANDARD_PREMIUM(1, "STANDARD_PREMIUM", "标准保费输入"),
    FIXED_AMOUNT(2, "FIXED_AMOUNT", "固定金额"),
    PERCENTAGE_OF(3, "PERCENTAGE_OF", "按前序金额比例"),
    SUM(4, "SUM", "合计");

    private final Integer enumCode;
    private final String code;
    private final String name;

    CalculationOperator(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    public static CalculationOperator fromCode(String code) {
        return BaseEnum.fromCode(CalculationOperator.class, code);
    }
}
