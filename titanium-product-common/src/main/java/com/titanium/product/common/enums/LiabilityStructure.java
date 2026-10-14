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
 * 🔴 <b>其余 3 值逐个定性（G11/g11-06，2026-10-10 实读核算，AC-08 收口）</b>：
 * </p>
 * <ul>
 *   <li>{@link #SINGLE}：<b>已接入</b>——数据承载面广（两测试数据文件 + 真机库 8 行模板，
 *       寿险/意外/医疗等单标产品均用本值），读写两侧（WebMapper.fromCode / 读侧回显）均可达，
 *       不标 {@link Reserved}；</li>
 *   <li>{@link #MAIN_ADDITIONAL}：<b>已接入</b>——生产写入侧构值
 *       （{@code ProductTemplateDataInitializer} 宠物模板）+ 真机库 1 行，不标 {@link Reserved}；</li>
 *   <li>{@link #MODULAR}：<b>零触达</b>——全仓 Java 零引用、两测试数据文件零出现、
 *       真机库零行（2026-10-10 精确口径核对），已标 {@link Reserved}
 *       （「标而不删」，触发条件见注解 value）。</li>
 * </ul>
 */
@Getter
public enum LiabilityStructure implements BaseEnum {
    @Reserved("2026-10-14 g16-05 零引用登记（枚举级消费）：存在 values/valueOf/自定方法/:: 调用，常量名零出现；接入时摘标注+删清单行。见 docs/技术文档/枚举零引用清单.md")
    SINGLE(1, "SINGLE", "单一责任"),
    MAIN_ADDITIONAL(2, "MAIN_ADDITIONAL", "主险+附加险"),
    @Reserved("2026-10-09 核对：全仓无消费方引用本值（生产零消费）；交强+商业同单已裁决走险别载体表达（G11 方案 B 2026-09-26 / g11-04），不经本值。边界：其余 3 值归 G11/AC-08 定性，本任务只管本值")
    PACKAGE(3, "PACKAGE", "套餐式"),
    @Reserved("2026-10-10 核对：全仓 Java 零引用、两测试数据文件零出现、真机库零行（G11/g11-06 精确口径）；模块化自选责任结构当前无任何产品形态使用。若启用须先建模块化产品形态（产品定义 + 出单装配 + 读侧展示），并把本值消费方接入后再摘本注解")
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
