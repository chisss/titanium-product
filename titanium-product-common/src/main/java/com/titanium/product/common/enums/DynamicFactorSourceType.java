package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * 动态因子原始特征来源。
 */
@Getter
public enum DynamicFactorSourceType implements BaseEnum {
    REQUEST(1, "REQUEST", "请求输入"),
    DOMAIN_API(2, "DOMAIN_API", "领域API"),
    DERIVED(3, "DERIVED", "加工特征"),
    EXTERNAL_REALTIME(4, "EXTERNAL_REALTIME", "外部实时");

    private final Integer enumCode;
    private final String code;
    private final String name;

    DynamicFactorSourceType(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    public static DynamicFactorSourceType fromCode(String code) {
        return BaseEnum.fromCode(DynamicFactorSourceType.class, code);
    }
}
