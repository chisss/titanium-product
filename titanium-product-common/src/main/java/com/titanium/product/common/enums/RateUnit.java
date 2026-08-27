package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * 费率单位。
 */
@Getter
public enum RateUnit implements BaseEnum {

    /** 费率直接乘以保额。 */
    SUM_INSURED_RATIO(1, "SUM_INSURED_RATIO", "保额比例", "保额直接乘以费率"),

    /** 每千元保额对应的费率。 */
    PER_THOUSAND_SUM_INSURED(2, "PER_THOUSAND_SUM_INSURED", "每千元保额", "每千元保额对应的费率"),

    /** 固定保费金额，不随保额变化。 */
    FIXED_AMOUNT(3, "FIXED_AMOUNT", "固定金额", "费率值直接作为固定保费金额");

    private final Integer enumCode;
    private final String code;
    private final String name;
    private final String desc;

    RateUnit(Integer enumCode, String code, String name, String desc) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
        this.desc = desc;
    }

    public static RateUnit fromCode(String code) {
        return BaseEnum.fromCode(RateUnit.class, code);
    }
}
