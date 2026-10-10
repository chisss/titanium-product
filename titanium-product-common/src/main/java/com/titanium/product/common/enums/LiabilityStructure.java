package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;
import com.titanium.metadata.enums.Reserved;

import lombok.Getter;

/**
 * 责任结构类型枚举
 * 定义保险产品的责任组合方式
 * <p>
 * 🔴 <b>触达现状（G10/g10-06 逐个核对，2026-10-09）</b>：本枚举作为
 * {@code PolicyStructureConfig.liabilityStructure} 的字段类型参与领域模型，写入侧经
 * {@code ProductTemplateWebMapper}（{@code fromCode}）与 {@code ProductTemplateDataInitializer}
 * （{@code MAIN_ADDITIONAL}）构值，并在两个 api response 中对外暴露；其中 {@link #PACKAGE}
 * 全仓无任何消费方引用（生产零消费），已标 {@link Reserved}。
 * </p>
 * <p>
 * 🔴 <b>边界</b>：其余 3 值（SINGLE / MAIN_ADDITIONAL / MODULAR）的逐个定性归 G11（AC-08），
 * 本任务（g10-06）只处置 PACKAGE。
 * </p>
 */
@Getter
public enum LiabilityStructure implements BaseEnum {
    SINGLE(1, "SINGLE", "单一责任"),
    MAIN_ADDITIONAL(2, "MAIN_ADDITIONAL", "主险+附加险"),
    @Reserved("2026-10-09 核对：全仓无消费方引用本值（生产零消费）；交强+商业同单已裁决走险别载体表达（G11 方案 B 2026-09-26 / g11-04），不经本值。边界：其余 3 值归 G11/AC-08 定性，本任务只管本值")
    PACKAGE(3, "PACKAGE", "套餐式"),
    MODULAR(4, "MODULAR", "模块化自选");

    private final Integer enumCode;
    private final String  code;
    private final String  name;

    LiabilityStructure(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    /**
     * 根据 code 反查枚举（统一范式入口，委托 {@link BaseEnum}）。
     *
     * @param code 责任结构编码
     * @return 匹配的枚举，未匹配返回 null
     */
    public static LiabilityStructure fromCode(String code) {
        return BaseEnum.fromCode(LiabilityStructure.class, code);
    }
}
