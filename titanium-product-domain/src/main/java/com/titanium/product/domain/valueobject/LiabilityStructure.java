package com.titanium.product.domain.valueobject;

import lombok.Getter;

/**
 * 责任结构类型枚举
 * 定义保险产品的责任组合方式
 */
@Getter
public enum LiabilityStructure {
    SINGLE("SINGLE", "单一责任"),
    MAIN_ADDITIONAL("MAIN_ADDITIONAL", "主险+附加险"),
    PACKAGE("PACKAGE", "套餐式"),
    MODULAR("MODULAR", "模块化自选");

    private final String code;
    private final String name;

    LiabilityStructure(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static LiabilityStructure fromCode(String code) {
        for (LiabilityStructure structure : values()) {
            if (structure.code.equals(code)) {
                return structure;
            }
        }
        return null;
    }
}
