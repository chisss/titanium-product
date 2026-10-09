package com.titanium.product.api.response.premium;

import java.math.BigDecimal;

/**
 * 退费因子（退保结算明细的单项）。
 * <p>
 * 🔴 字段一律用**字符串码**而非枚举：跨域契约以 {@code name()}/{@code code} 传输，
 * 枚举常量名即契约（改名等于破坏对端），且避免把本域枚举序列化进对端。
 * </p>
 *
 * @param factorType 因子类型（CASH_VALUE / UNEARNED_PREMIUM / LOAN_PRINCIPAL_INTEREST /
 *                   OVERDUE_PREMIUM / DIVIDEND_ACCOUNT_VALUE / HANDLING_FEE）
 * @param direction  金额方向（CREDIT 加项 / DEBIT 扣项 / NONE 该产品不适用），金额本身为非负绝对值
 * @param amount     金额绝对值
 * @param explainKey 文案键（展示层按 locale 渲染，形如 {@code surrender.factor.cashValue}）
 */
public record SurrenderFactorResponse(
        String factorType,
        String direction,
        BigDecimal amount,
        String explainKey) {
}
