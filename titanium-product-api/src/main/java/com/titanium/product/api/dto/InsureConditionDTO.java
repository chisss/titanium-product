package com.titanium.product.api.dto;

import lombok.Data;

/**
 * 投保条件DTO
 * 用于表示产品的投保条件
 */
@Data
public class InsureConditionDTO {
    /**
     * 最小年龄
     */
    private Integer minAge;

    /**
     * 最大年龄
     */
    private Integer maxAge;

    /**
     * 职业限制（JSON格式）
     */
    private String occupationRestrictions;

    /**
     * 健康告知要求（JSON格式）
     */
    private String healthDeclaration;

    /**
     * 地域限制（JSON格式）
     */
    private String regionRestrictions;
}
