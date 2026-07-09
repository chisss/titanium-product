package com.titanium.product.web.request;

import java.util.List;

import lombok.Data;

/**
 * 投保条件请求（后台/端上 HTTP 入参）
 * <p>
 * 面向管理后台/端上的产品投保条件请求，由 {@code ProductWebMapper} 翻译为领域值对象 {@code InsureCondition}。
 * </p>
 */
@Data
public class InsureConditionRequest {

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
}
