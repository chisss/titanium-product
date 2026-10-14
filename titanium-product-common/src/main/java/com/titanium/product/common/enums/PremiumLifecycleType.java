package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;
import com.titanium.metadata.enums.Reserved;

import lombok.Getter;

/**
 * 保单生命周期费用计算类型。
 */
@Getter
public enum PremiumLifecycleType implements BaseEnum {
    @Reserved("2026-10-14 g16-05 零引用登记（枚举级消费）：存在 values/valueOf/自定方法/:: 调用，常量名零出现；接入时摘标注+删清单行。见 docs/技术文档/枚举零引用清单.md")
    ENDORSEMENT(1, "ENDORSEMENT", "批改"),
    @Reserved("2026-10-14 g16-05 零引用登记（枚举级消费）：存在 values/valueOf/自定方法/:: 调用，常量名零出现；接入时摘标注+删清单行。见 docs/技术文档/枚举零引用清单.md")
    RENEWAL(2, "RENEWAL", "续期"),
    SURRENDER(3, "SURRENDER", "退保"),
    REVERSAL(4, "REVERSAL", "冲正");

    private final Integer enumCode;
    private final String code;
    private final String name;

    PremiumLifecycleType(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    public static PremiumLifecycleType fromCode(String code) {
        return BaseEnum.fromCode(PremiumLifecycleType.class, code);
    }
}
