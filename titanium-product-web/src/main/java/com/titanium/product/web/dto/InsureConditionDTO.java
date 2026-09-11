package com.titanium.product.web.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

/**
 * 投保条件请求（后台/端上 HTTP 入参）
 * <p>
 * 面向管理后台/端上的产品投保条件请求，由 {@code ProductWebMapper} 翻译为领域值对象 {@code InsureCondition}。
 * </p>
 * <p>
 * 🔴 <b>字段集与领域值对象对齐</b>：DTO 字段名必须全部存在于 {@link InsureCondition}，否则 MapStruct
 * 找不到目标属性会<b>静默丢弃</b>该入参（既不报错也不落库）。历史上
 * {@code occupationRestrictions}/{@code healthDeclaration}/{@code regionRestrictions} 三个 JSON 字符串字段
 * 即属此列（其结构化语义已由 allowed/forbiddenOccupations 与 allowed/forbiddenRegions 等类型化列表承载），
 * 已清除。该约束由 {@code ProductNestedInputDTOContractTest} 反射断言固化。
 * </p>
 *
 * @see com.titanium.product.valueobject.config.InsureCondition
 */
@Data
public class InsureConditionDTO {

    /** 最小年龄 */
    private Integer minAge;
    /** 最大年龄 */
    private Integer maxAge;
    /** 禁止职业列表 */
    private List<String> forbiddenOccupations;
    /** 允许职业列表（白名单模式） */
    private List<String> allowedOccupations;
    /** 最小团体人数 */
    private Integer minGroupSize;
    /** 最大团体人数 */
    private Integer maxGroupSize;
    /** 健康告知 */
    private String healthNotice;
    /** 最小保额 */
    private BigDecimal minInsuredAmount;
    /** 最大保额 */
    private BigDecimal maxInsuredAmount;
    /** 禁止地域列表 */
    private List<String> forbiddenRegions;
    /** 允许地域列表 */
    private List<String> allowedRegions;
    /** 最大投保人数限制 */
    private Integer maxInsuredCount;
    /** 等待期（天） */
    private Integer waitingPeriodDays;
    /** 犹豫期（天） */
    private Integer hesitationPeriodDays;
}
