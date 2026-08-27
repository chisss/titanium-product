package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/** Product 保全 Offering 业务失败原因。 */
@Getter
public enum ProductMaintenanceOfferingFailureReason implements BaseEnum {
    NOT_FOUND(1, "NOT_FOUND", "Offering不存在"),
    ALREADY_EXISTS(2, "ALREADY_EXISTS", "Offering版本已存在"),
    VERSION_MISMATCH(3, "VERSION_MISMATCH", "产品或计划版本不匹配"),
    NOT_APPLICABLE(4, "NOT_APPLICABLE", "Offering不适用于当前案件"),
    PERIOD_CONFLICT(5, "PERIOD_CONFLICT", "Offering有效期冲突"),
    STATE_INVALID(6, "STATE_INVALID", "Offering状态不允许当前操作"),
    CONTRACT_INVALID(7, "CONTRACT_INVALID", "Offering契约无效");

    private final Integer enumCode;
    private final String code;
    private final String name;

    ProductMaintenanceOfferingFailureReason(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }
}
