package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * 保单生命周期费用计算类型。
 */
@Getter
public enum PremiumLifecycleType implements BaseEnum {
    ENDORSEMENT(1, "ENDORSEMENT", "批改"),
    RENEWAL(2, "RENEWAL", "续期"),
    SURRENDER(3, "SURRENDER", "退保"),
    REVERSAL(4, "REVERSAL", "冲正");

    private final Integer enumCode;
    private final String code;
    private final String name;

    PremiumLifecycleType(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    public static PremiumLifecycleType fromCode(String code) {
        return BaseEnum.fromCode(PremiumLifecycleType.class, code);
    }
}
