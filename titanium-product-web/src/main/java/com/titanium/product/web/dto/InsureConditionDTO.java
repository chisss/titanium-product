package com.titanium.product.web.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

/**
 * 投保条件请求（后台/端上 HTTP 入参）
 * <p>
 * 面向管理后台/端上的产品投保条件请求，由 {@code ProductWebMapper} 翻译为领域值对象 {@code InsureCondition}。
 * </p>
 */
@Data
public class InsureConditionDTO {

    /** 最小年龄 */
    private Integer minAge;
    /** 最大年龄 */
    private Integer maxAge;
    /** 禁止职业列表 */
    private List<String> forbiddenOccupations;
    /** 职业限制（JSON格式） */
    private String occupationRestrictions;
    /** 健康告知要求（JSON格式） */
    private String healthDeclaration;
    /** 地域限制（JSON格式） */
    private String regionRestrictions;
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
    /** 允许职业列表（白名单模式） */
    private List<String> allowedOccupations;
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
