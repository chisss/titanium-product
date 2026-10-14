package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;
import com.titanium.metadata.enums.Reserved;

import lombok.Getter;

/**
 * 动态因子原始特征来源。
 */
@Getter
public enum DynamicFactorSourceType implements BaseEnum {
    @Reserved("2026-10-14 g16-05 零引用登记（枚举级消费）：存在 values/valueOf/自定方法/:: 调用，常量名零出现；接入时摘标注+删清单行。见 docs/技术文档/枚举零引用清单.md")
    REQUEST(1, "REQUEST", "请求输入"),
    @Reserved("2026-10-14 g16-05 零引用登记（枚举级消费）：存在 values/valueOf/自定方法/:: 调用，常量名零出现；接入时摘标注+删清单行。见 docs/技术文档/枚举零引用清单.md")
    DOMAIN_API(2, "DOMAIN_API", "领域API"),
    @Reserved("2026-10-14 g16-05 零引用登记（枚举级消费）：存在 values/valueOf/自定方法/:: 调用，常量名零出现；接入时摘标注+删清单行。见 docs/技术文档/枚举零引用清单.md")
    DERIVED(3, "DERIVED", "加工特征"),
    @Reserved("2026-10-14 g16-05 零引用登记（枚举级消费）：存在 values/valueOf/自定方法/:: 调用，常量名零出现；接入时摘标注+删清单行。见 docs/技术文档/枚举零引用清单.md")
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
