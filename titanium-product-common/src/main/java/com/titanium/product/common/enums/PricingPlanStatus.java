package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * Product 定价方案生命周期状态。
 */
@Getter
public enum PricingPlanStatus implements BaseEnum {
    DRAFT(1, "DRAFT", "草稿"),
    APPROVED(2, "APPROVED", "已审批"),
    PUBLISHED(3, "PUBLISHED", "已发布"),
    RETIRED(4, "RETIRED", "已退役");

    private final Integer enumCode;
    private final String code;
    private final String name;

    PricingPlanStatus(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    public static PricingPlanStatus fromCode(String code) {
        return BaseEnum.fromCode(PricingPlanStatus.class, code);
    }
}
