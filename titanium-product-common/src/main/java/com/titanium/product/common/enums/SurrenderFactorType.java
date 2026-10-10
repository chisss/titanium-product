package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * 退费因子类型：退保结算明细的构成项。
 * <p>
 * 退费金额不再由「已缴保费 × 单一退费率」一次算出，而是由若干**因子加总**得出
 * （净额 = ΣCREDIT − ΣDEBIT，见 {@code SurrenderRefundBreakdown}）。本枚举是因子词表的
 * 唯一权威，新增因子类型须同批补：值对象加总逻辑、读模型明细、以及后台字典登记。
 * </p>
 * <p>
 * 归属判据（项目 CLAUDE.md §3.4.2）：退费因子的解释权在**产品域**（退费口径权威域），
 * 故落 product-common 而非 metadata；保全域经 Feign 收字符串后按其自有防腐类型承接。
 * </p>
 */
@Getter
public enum SurrenderFactorType implements BaseEnum {

    /** 现金价值（犹豫期内为全额保费口径，两者由 explainKey 区分） */
    CASH_VALUE(1, "CASH_VALUE", "现金价值"),
    /** 未满期保费：保单年度剩余期间对应的已赚/未赚保费部分 */
    UNEARNED_PREMIUM(2, "UNEARNED_PREMIUM", "未满期保费"),
    /** 保单贷款未偿本息：退费时先行抵扣 */
    LOAN_PRINCIPAL_INTEREST(3, "LOAN_PRINCIPAL_INTEREST", "保单贷款未偿本息"),
    /** 宽限期内欠缴保费：退费时先行抵扣 */
    OVERDUE_PREMIUM(4, "OVERDUE_PREMIUM", "欠缴保费"),
    /** 红利/账户价值（分红险、万能险的客户权益余额） */
    DIVIDEND_ACCOUNT_VALUE(5, "DIVIDEND_ACCOUNT_VALUE", "红利及账户价值"),
    /** 手续费：退保受理费用，来源为产品退保策略配置 */
    HANDLING_FEE(6, "HANDLING_FEE", "退保手续费");

    private final Integer enumCode;
    private final String code;
    private final String name;

    SurrenderFactorType(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    public static SurrenderFactorType fromCode(String code) {
        return BaseEnum.fromCode(SurrenderFactorType.class, code);
    }
}
