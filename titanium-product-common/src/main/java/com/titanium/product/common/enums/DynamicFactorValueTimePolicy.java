package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;
import com.titanium.metadata.enums.Reserved;

import lombok.Getter;

/**
 * 动态因子取值时点策略。
 */
@Getter
public enum DynamicFactorValueTimePolicy implements BaseEnum {
    @Reserved("2026-10-14 g16-05 零引用登记（枚举级消费）：存在 values/valueOf/自定方法/:: 调用，常量名零出现；接入时摘标注+删清单行。见 docs/技术文档/枚举零引用清单.md")
    REQUEST_TIME(1, "REQUEST_TIME", "请求时点"),
    @Reserved("2026-10-14 g16-05 零引用登记（枚举级消费）：存在 values/valueOf/自定方法/:: 调用，常量名零出现；接入时摘标注+删清单行。见 docs/技术文档/枚举零引用清单.md")
    BUSINESS_TIME(2, "BUSINESS_TIME", "业务时点"),
    @Reserved("2026-10-14 g16-05 零引用登记（枚举级消费）：存在 values/valueOf/自定方法/:: 调用，常量名零出现；接入时摘标注+删清单行。见 docs/技术文档/枚举零引用清单.md")
    OBSERVED_AT(3, "OBSERVED_AT", "观测时点");

    private final Integer enumCode;
    private final String code;
    private final String name;

    DynamicFactorValueTimePolicy(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    public static DynamicFactorValueTimePolicy fromCode(String code) {
        return BaseEnum.fromCode(DynamicFactorValueTimePolicy.class, code);
    }
}
