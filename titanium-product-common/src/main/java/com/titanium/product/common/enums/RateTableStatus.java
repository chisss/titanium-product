package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * 产品费率表状态。
 */
@Getter
public enum RateTableStatus implements BaseEnum {
    DRAFT(1, "DRAFT", "草稿"),
    PUBLISHED(2, "PUBLISHED", "已发布"),
    RETIRED(3, "RETIRED", "已退役");

    private final Integer enumCode;
    private final String code;
    private final String name;

    RateTableStatus(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    public static RateTableStatus fromCode(String code) {
        return BaseEnum.fromCode(RateTableStatus.class, code);
    }
}
