package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * Product 确认计算业务用途。
 */
@Getter
public enum PricingCalculationPurpose implements BaseEnum {
    ISSUANCE_CONFIRM(1, "ISSUANCE_CONFIRM", "出单确认"),
    MAINTENANCE(2, "MAINTENANCE", "保全计算");

    private final Integer enumCode;
    private final String code;
    private final String name;

    PricingCalculationPurpose(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    public static PricingCalculationPurpose fromCode(String code) {
        return BaseEnum.fromCode(PricingCalculationPurpose.class, code);
    }
}
