package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * 精算计算图节点类型。
 */
@Getter
public enum CalculationNodeType implements BaseEnum {
    INPUT(1, "INPUT", "输入"),
    COMPUTE(2, "COMPUTE", "计算"),
    AGGREGATE(3, "AGGREGATE", "汇总"),
    OUTPUT(4, "OUTPUT", "输出");

    private final Integer enumCode;
    private final String code;
    private final String name;

    CalculationNodeType(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    public static CalculationNodeType fromCode(String code) {
        return BaseEnum.fromCode(CalculationNodeType.class, code);
    }
}
