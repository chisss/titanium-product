package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * 精算配置统一生命周期状态。
 */
@Getter
public enum ActuarialDefinitionStatus implements BaseEnum {
    DRAFT(1, "DRAFT", "草稿"),
    APPROVED(2, "APPROVED", "已审批"),
    PUBLISHED(3, "PUBLISHED", "已发布"),
    RETIRED(4, "RETIRED", "已退役");

    private final Integer enumCode;
    private final String code;
    private final String name;

    ActuarialDefinitionStatus(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    public static ActuarialDefinitionStatus fromCode(String code) {
        return BaseEnum.fromCode(ActuarialDefinitionStatus.class, code);
    }
}
