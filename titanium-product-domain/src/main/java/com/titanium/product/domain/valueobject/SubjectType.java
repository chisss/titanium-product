package com.titanium.product.domain.valueobject;

import lombok.Getter;

/**
 * 标的类型枚举
 * 定义保险产品标的对象的类型
 */
@Getter
public enum SubjectType {
    PERSON("PERSON", "人"),
    VEHICLE("VEHICLE", "车辆"),
    PET("PET", "宠物"),
    PROPERTY("PROPERTY", "财产"),
    CARGO("CARGO", "货物"),
    HOUSEHOLD("HOUSEHOLD", "家庭"),
    ORGANIZATION("ORGANIZATION", "机构"),
    AGRICULTURAL("AGRICULTURAL", "农业"),
    VESSEL("VESSEL", "船舶"),
    AIRCRAFT("AIRCRAFT", "航空"),
    PERSON_OR_ORGANIZATION("PERSON_OR_ORGANIZATION", "个人或机构");

    private final String code;
    private final String name;

    SubjectType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static SubjectType fromCode(String code) {
        for (SubjectType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
