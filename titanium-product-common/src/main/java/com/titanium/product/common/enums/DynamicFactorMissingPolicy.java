package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * 动态因子缺失值处理策略。
 */
@Getter
public enum DynamicFactorMissingPolicy implements BaseEnum {
    REJECT(1, "REJECT", "拒绝计算"),
    USE_DEFAULT(2, "USE_DEFAULT", "使用默认值"),
    SKIP(3, "SKIP", "跳过因子");

    private final Integer enumCode;
    private final String code;
    private final String name;

    DynamicFactorMissingPolicy(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    public static DynamicFactorMissingPolicy fromCode(String code) {
        return BaseEnum.fromCode(DynamicFactorMissingPolicy.class, code);
    }
}
