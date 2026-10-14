package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;
import com.titanium.metadata.enums.Reserved;

import lombok.Getter;

/**
 * 费用项计算来源。
 */
@Getter
public enum ChargeCalculationSource implements BaseEnum {
    @Reserved("2026-10-14 g16-05 零引用登记（枚举级消费）：存在 values/valueOf/自定方法/:: 调用，常量名零出现；接入时摘标注+删清单行。见 docs/技术文档/枚举零引用清单.md")
    BASE_PREMIUM(1, "BASE_PREMIUM", "基础保费"),
    @Reserved("2026-10-14 g16-05 零引用登记（枚举级消费）：存在 values/valueOf/自定方法/:: 调用，常量名零出现；接入时摘标注+删清单行。见 docs/技术文档/枚举零引用清单.md")
    FIXED_AMOUNT(2, "FIXED_AMOUNT", "固定金额"),
    @Reserved("2026-10-14 g16-05 零引用登记（枚举级消费）：存在 values/valueOf/自定方法/:: 调用，常量名零出现；接入时摘标注+删清单行。见 docs/技术文档/枚举零引用清单.md")
    PERCENTAGE(3, "PERCENTAGE", "比例计算"),
    @Reserved("2026-10-14 g16-05 零引用登记（枚举级消费）：存在 values/valueOf/自定方法/:: 调用，常量名零出现；接入时摘标注+删清单行。见 docs/技术文档/枚举零引用清单.md")
    RATE_TABLE(4, "RATE_TABLE", "费率表"),
    @Reserved("2026-10-14 g16-05 零引用登记（枚举级消费）：存在 values/valueOf/自定方法/:: 调用，常量名零出现；接入时摘标注+删清单行。见 docs/技术文档/枚举零引用清单.md")
    FORMULA(5, "FORMULA", "公式工件"),
    @Reserved("2026-10-14 g16-05 零引用登记（枚举级消费）：存在 values/valueOf/自定方法/:: 调用，常量名零出现；接入时摘标注+删清单行。见 docs/技术文档/枚举零引用清单.md")
    EXTERNAL_RESULT(6, "EXTERNAL_RESULT", "外部结果");

    private final Integer enumCode;
    private final String code;
    private final String name;

    ChargeCalculationSource(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    public static ChargeCalculationSource fromCode(String code) {
        return BaseEnum.fromCode(ChargeCalculationSource.class, code);
    }
}
