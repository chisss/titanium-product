package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * 动态因子取值时点策略。
 */
@Getter
public enum DynamicFactorValueTimePolicy implements BaseEnum {
    REQUEST_TIME(1, "REQUEST_TIME", "请求时点"),
    BUSINESS_TIME(2, "BUSINESS_TIME", "业务时点"),
    OBSERVED_AT(3, "OBSERVED_AT", "观测时点");

    private final Integer enumCode;
    private final String code;
    private final String name;

    DynamicFactorValueTimePolicy(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    public static DynamicFactorValueTimePolicy fromCode(String code) {
        return BaseEnum.fromCode(DynamicFactorValueTimePolicy.class, code);
    }
}
