package com.titanium.product.domain.valueobject;

import lombok.Getter;

/**
 * 出单模式枚举
 * 定义产品的出单流程步骤数
 */
@Getter
public enum IssuanceMode {
    ONE_STEP("ONE_STEP", "一步出单", "报价即保单，无需核保"),
    TWO_STEP("TWO_STEP", "两步出单", "投保单→保单，自动核保"),
    THREE_STEP("THREE_STEP", "三步出单", "意向单→投保单→保单，人工核保");

    private final String code;
    private final String name;
    private final String description;

    IssuanceMode(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public static IssuanceMode fromCode(String code) {
        for (IssuanceMode mode : values()) {
            if (mode.code.equals(code)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("未知的出单模式: " + code);
    }
}
