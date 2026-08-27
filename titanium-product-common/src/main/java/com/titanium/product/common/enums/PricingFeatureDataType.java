package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * Product 定价契约支持的特征数据类型。
 */
@Getter
public enum PricingFeatureDataType implements BaseEnum {
    STRING(1, "STRING", "字符串", "字符串特征值"),
    INTEGER(2, "INTEGER", "整数", "整数特征值"),
    DECIMAL(3, "DECIMAL", "小数", "高精度小数特征值"),
    BOOLEAN(4, "BOOLEAN", "布尔值", "真假特征值"),
    DATE(5, "DATE", "日期", "不含时区的日期特征值"),
    DATETIME(6, "DATETIME", "日期时间", "不含时区的日期时间特征值"),
    ENUM(7, "ENUM", "枚举", "稳定业务编码特征值"),
    JSON(8, "JSON", "JSON", "序列化后的结构化特征值");

    private final Integer enumCode;
    private final String code;
    private final String name;
    private final String desc;

    PricingFeatureDataType(Integer enumCode, String code, String name, String desc) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
        this.desc = desc;
    }

    public static PricingFeatureDataType fromCode(String code) {
        return BaseEnum.fromCode(PricingFeatureDataType.class, code);
    }
}
