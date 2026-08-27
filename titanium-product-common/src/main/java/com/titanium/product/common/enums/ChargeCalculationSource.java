package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * 费用项计算来源。
 */
@Getter
public enum ChargeCalculationSource implements BaseEnum {
    BASE_PREMIUM(1, "BASE_PREMIUM", "基础保费"),
    FIXED_AMOUNT(2, "FIXED_AMOUNT", "固定金额"),
    PERCENTAGE(3, "PERCENTAGE", "比例计算"),
    RATE_TABLE(4, "RATE_TABLE", "费率表"),
    FORMULA(5, "FORMULA", "公式工件"),
    EXTERNAL_RESULT(6, "EXTERNAL_RESULT", "外部结果");

    private final Integer enumCode;
    private final String code;
    private final String name;

    ChargeCalculationSource(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    public static ChargeCalculationSource fromCode(String code) {
        return BaseEnum.fromCode(ChargeCalculationSource.class, code);
    }
}
