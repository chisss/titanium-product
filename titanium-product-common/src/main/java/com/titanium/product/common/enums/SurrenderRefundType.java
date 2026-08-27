package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/** Product 退保价值计算使用的退费类型。 */
@Getter
public enum SurrenderRefundType implements BaseEnum {
    COOLING_OFF_FULL_REFUND(1, "COOLING_OFF_FULL_REFUND", "犹豫期全额退费"),
    CASH_VALUE(2, "CASH_VALUE", "现金价值退费");

    private final Integer enumCode;
    private final String code;
    private final String name;

    SurrenderRefundType(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    public static SurrenderRefundType fromCode(String code) {
        return BaseEnum.fromCode(SurrenderRefundType.class, code);
    }
}
