package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * 动态因子数值变换类型。
 */
@Getter
public enum DynamicFactorTransformType implements BaseEnum {
    IDENTITY(1, "IDENTITY", "原值"),
    LINEAR(2, "LINEAR", "线性变换");

    private final Integer enumCode;
    private final String code;
    private final String name;

    DynamicFactorTransformType(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    public static DynamicFactorTransformType fromCode(String code) {
        return BaseEnum.fromCode(DynamicFactorTransformType.class, code);
    }
}
