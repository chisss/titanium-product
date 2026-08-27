package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * 生命周期差额对客户余额的影响方向。
 */
@Getter
public enum PremiumBalanceDirection implements BaseEnum {
    DEBIT(1, "DEBIT", "追加应收"),
    CREDIT(2, "CREDIT", "应退或贷项"),
    NONE(3, "NONE", "无金额变化");

    private final Integer enumCode;
    private final String code;
    private final String name;

    PremiumBalanceDirection(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    public static PremiumBalanceDirection fromCode(String code) {
        return BaseEnum.fromCode(PremiumBalanceDirection.class, code);
    }
}
