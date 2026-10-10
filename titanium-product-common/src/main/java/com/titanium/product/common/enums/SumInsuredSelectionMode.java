package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * 保额录入方式枚举（选档 vs 自由填）
 * <p>
 * 表达「投保时录入的责任保额」是如何被确定的，供投保单录入侧（消费方 G02）分派后续校验：
 * 选档须回填命中的档位，自由填则只受区间约束、无档位可回填。
 * </p>
 * <p>
 * 按项目规约 §3.4.2，本域专属枚举统一置于 {@code product-common/enums}，业务包内不定义枚举；
 * 并按《DDD-枚举定义规范与BaseEnum基类指南.md》实现 {@link BaseEnum}，取得标准四属性
 * （{@code enumCode}/{@code code}/{@code name}/{@code desc}）与内聚的 {@code fromCode}/i18nKey。
 * </p>
 */
@Getter
public enum SumInsuredSelectionMode implements BaseEnum {

    /** 选档：录入的保额精确等于某个档位保额，命中该档位 */
    TIER(1, "TIER", "选档"),

    /** 自由填：录入的保额落在允许区间内但不对应任何档位 */
    CUSTOM(2, "CUSTOM", "自由填");

    private final Integer enumCode;
    private final String  code;
    private final String  name;

    SumInsuredSelectionMode(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    /**
     * 根据 code 反查枚举（统一范式入口，委托 {@link BaseEnum}）。
     *
     * @param code 录入方式编码
     * @return 匹配的枚举，未匹配返回 null
     */
    public static SumInsuredSelectionMode fromCode(String code) {
        return BaseEnum.fromCode(SumInsuredSelectionMode.class, code);
    }
}
