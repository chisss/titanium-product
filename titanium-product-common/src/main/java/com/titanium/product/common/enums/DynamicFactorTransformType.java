package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;
import com.titanium.metadata.enums.Reserved;

import lombok.Getter;

/**
 * 动态因子数值变换类型。
 */
@Getter
public enum DynamicFactorTransformType implements BaseEnum {
    IDENTITY(1, "IDENTITY", "原值"),
    @Reserved("2026-10-14 g16-05 零引用登记（枚举级消费）：存在 values/valueOf/自定方法/:: 调用，常量名零出现；接入时摘标注+删清单行。见 docs/技术文档/枚举零引用清单.md")
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
