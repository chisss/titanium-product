package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * 确认计算的结构化保费调整类型。
 */
@Getter
public enum PremiumAdjustmentType implements BaseEnum {
    SURCHARGE_RATE(1, "SURCHARGE_RATE", "比例加费"),
    DISCOUNT_RATE(2, "DISCOUNT_RATE", "比例折扣"),
    SURCHARGE_AMOUNT(3, "SURCHARGE_AMOUNT", "定额加费"),
    DISCOUNT_AMOUNT(4, "DISCOUNT_AMOUNT", "定额折扣");

    private final Integer enumCode;
    private final String code;
    private final String name;

    PremiumAdjustmentType(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    public static PremiumAdjustmentType fromCode(String code) {
        return BaseEnum.fromCode(PremiumAdjustmentType.class, code);
    }
}
