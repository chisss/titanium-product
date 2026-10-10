package com.titanium.product.common.enums;

import com.titanium.metadata.enums.BaseEnum;

import lombok.Getter;

/**
 * 退费基准：退保应退金额以哪一类客户权益为计算基准。
 * <p>
 * 🔴 <b>两者择一，不得同时生效</b>（g05-02 业务裁示）：长期险按现金价值退保、一年期短期险按
 * 未满期保费退保，同一张保单只可能落在其中一种口径上。若把两者都做成加项，退费净额会超过
 * 客户实付（现金价值 + 未满期保费 &gt; 已缴保费），客户保留金额为负而被
 * {@code SurrenderRefundBreakdown} 拒绝——按 0.6 现金价值率测算，全年约 40% 的时间窗内退保
 * 都会失败。故本枚举表达「选哪一个」，而非「开关」。
 * </p>
 * <p>
 * 两个因子**始终出现在退费明细中**（付费期满、趸缴、未生效基准为 0），使「该项为 0」与
 * 「该项缺失」在明细上可区分——明细缺项会让运营无法判断是口径不适用还是取数失败。
 * </p>
 * <p>
 * 归属判据（项目 CLAUDE.md §3.4.2）：退费口径的解释权在**产品域**，故落 product-common 而非
 * metadata；保全域经 Feign 收字符串码后按其自有防腐类型承接。
 * </p>
 */
@Getter
public enum SurrenderRefundBasis implements BaseEnum {

    /** 现金价值基准：长期险口径，应退金额 = 客户应付 × 现金价值率 */
    CASH_VALUE(1, "CASH_VALUE", "现金价值基准"),
    /** 未满期保费基准：一年期短期险口径，应退金额 = 年保费 × 剩余保障天数 / 365 */
    UNEARNED_PREMIUM(2, "UNEARNED_PREMIUM", "未满期保费基准");

    private final Integer enumCode;
    private final String code;
    private final String name;

    SurrenderRefundBasis(Integer enumCode, String code, String name) {
        this.enumCode = enumCode;
        this.code = code;
        this.name = name;
    }

    public static SurrenderRefundBasis fromCode(String code) {
        return BaseEnum.fromCode(SurrenderRefundBasis.class, code);
    }
}
