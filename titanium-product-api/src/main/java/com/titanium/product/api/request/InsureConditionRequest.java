package com.titanium.product.api.request;

import java.util.List;

import lombok.Data;

/**
 * 投保条件请求 用于产品投保条件的请求参数
 */
@Data
public class InsureConditionRequest {
    /**
     * 最小年龄
     */
    private Integer      minAge;

    /**
     * 最大年龄
     */
    private Integer      maxAge;
    private List<String> forbiddenOccupations;

    /**
     * 职业限制（JSON格式）
     */
    private String       occupationRestrictions;

    /**
     * 健康告知要求（JSON格式）
     */
    private String       healthDeclaration;

    /**
     * 地域限制（JSON格式）
     */
    private String       regionRestrictions;

    private Integer      minGroupSize;
    private Integer      maxGroupSize;
    private String       healthNotice;
}
