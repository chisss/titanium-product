package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * 责任结构类型枚举
 * 定义保险产品的责任组合方式
 */
@Getter
public enum LiabilityStructure implements BaseEnum {
    SINGLE(1, "SINGLE", "单一责任"),
    MAIN_ADDITIONAL(2, "MAIN_ADDITIONAL", "主险+附加险"),
    PACKAGE(3, "PACKAGE", "套餐式"),
    MODULAR(4, "MODULAR", "模块化自选");

    private final Integer enumCode;
    private final String  code;
    private final String  name;

    LiabilityStructure(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    /**
     * 根据 code 反查枚举（统一范式入口，委托 {@link BaseEnum}）。
     *
     * @param code 责任结构编码
     * @return 匹配的枚举，未匹配返回 null
     */
    public static LiabilityStructure fromCode(String code) {
        return BaseEnum.fromCode(LiabilityStructure.class, code);
    }
}
