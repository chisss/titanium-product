package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * Product 确认计算状态。
 */
@Getter
public enum PricingCalculationStatus implements BaseEnum {
    CONFIRMED(1, "CONFIRMED", "已确认");

    private final Integer enumCode;
    private final String code;
    private final String name;

    PricingCalculationStatus(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    public static PricingCalculationStatus fromCode(String code) {
        return BaseEnum.fromCode(PricingCalculationStatus.class, code);
    }
}
