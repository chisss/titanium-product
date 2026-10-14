package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;
import com.titanium.metadata.enums.Reserved;

import lombok.Getter;

/**
 * 精算计算图节点类型。
 */
@Getter
public enum CalculationNodeType implements BaseEnum {
    INPUT(1, "INPUT", "输入"),
    @Reserved("2026-10-14 g16-05 零引用登记（枚举级消费）：存在 values/valueOf/自定方法/:: 调用，常量名零出现；接入时摘标注+删清单行。见 docs/技术文档/枚举零引用清单.md")
    COMPUTE(2, "COMPUTE", "计算"),
    @Reserved("2026-10-14 g16-05 零引用登记（枚举级消费）：存在 values/valueOf/自定方法/:: 调用，常量名零出现；接入时摘标注+删清单行。见 docs/技术文档/枚举零引用清单.md")
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
