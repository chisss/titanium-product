package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * 标的类型枚举
 * 定义保险产品标的对象的类型
 */
@Getter
public enum SubjectType implements BaseEnum {
    PERSON(1, "PERSON", "人"),
    VEHICLE(2, "VEHICLE", "车辆"),
    PET(3, "PET", "宠物"),
    PROPERTY(4, "PROPERTY", "财产"),
    CARGO(5, "CARGO", "货物"),
    HOUSEHOLD(6, "HOUSEHOLD", "家庭"),
    ORGANIZATION(7, "ORGANIZATION", "机构"),
    AGRICULTURAL(8, "AGRICULTURAL", "农业"),
    VESSEL(9, "VESSEL", "船舶"),
    AIRCRAFT(10, "AIRCRAFT", "航空"),
    PERSON_OR_ORGANIZATION(11, "PERSON_OR_ORGANIZATION", "个人或机构");

    private final Integer enumCode;
    private final String  code;
    private final String  name;

    SubjectType(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    /**
     * 根据 code 反查枚举（统一范式入口，委托 {@link BaseEnum}）。
     *
     * @param code 标的类型编码
     * @return 匹配的枚举，未匹配返回 null
     */
    public static SubjectType fromCode(String code) {
        return BaseEnum.fromCode(SubjectType.class, code);
    }
}
