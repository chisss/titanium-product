package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/** Product 保全 Offering 生命周期状态。 */
@Getter
public enum ProductMaintenanceOfferingStatus implements BaseEnum {
    DRAFT(1, "DRAFT", "草稿"),
    PUBLISHED(2, "PUBLISHED", "已发布"),
    RETIRED(3, "RETIRED", "已退役");

    private final Integer enumCode;
    private final String code;
    private final String name;

    ProductMaintenanceOfferingStatus(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }
}
