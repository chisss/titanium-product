package com.titanium.product.valueobject.pricing.premium;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.common.enums.SurrenderFactorType;
import com.titanium.product.exception.PricingDomainException;

/**
 * 退费因子值对象契约测试（g05-01）
 * <p>
 * 固化三条不变量：金额恒为非负绝对值（增减由 {@link ChargeDirection} 表达）、方向 NONE 必须零额、
 * 文案键禁止中文字面量（红线 20：文案由展示层按 locale 渲染，不得写进领域模型）。
 * </p>
 */
class SurrenderFactorTest {

    @Test
    @DisplayName("带符号贡献：CREDIT 取正、DEBIT 取负、NONE 归零")
    void signedAmountFollowsDirection() {
        assertEquals(0, credit(new BigDecimal("120.00")).signedAmount().compareTo(new BigDecimal("120.00")));
        assertEquals(0, debit(new BigDecimal("20.00")).signedAmount().compareTo(new BigDecimal("-20.00")));
        assertEquals(0, none().signedAmount().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("金额为负即拒绝：符号只能由 direction 表达，不得写进 amount")
    void shouldRejectNegativeAmount() {
        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> new SurrenderFactor(SurrenderFactorType.HANDLING_FEE, ChargeDirection.DEBIT,
                        new BigDecimal("-1"), "surrender.factor.handlingFee"));

        assertEquals(ProductErrorCode.SURRENDER_FACTOR_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("文案键含中文字面量即拒绝（红线 20：落库/跨域文案必须键化）")
    void shouldRejectChineseExplainKey() {
        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> new SurrenderFactor(SurrenderFactorType.CASH_VALUE, ChargeDirection.CREDIT,
                        BigDecimal.TEN, "现金价值"));

        assertEquals(ProductErrorCode.SURRENDER_FACTOR_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("文案键空白即拒绝")
    void shouldRejectBlankExplainKey() {
        assertThrows(PricingDomainException.class, () -> new SurrenderFactor(SurrenderFactorType.CASH_VALUE,
                ChargeDirection.CREDIT, BigDecimal.TEN, "   "));
    }

    @Test
    @DisplayName("方向 NONE 表示「该产品不适用」，金额非零即拒绝")
    void shouldRejectNonZeroAmountWhenDirectionIsNone() {
        PricingDomainException ex = assertThrows(PricingDomainException.class,
                () -> new SurrenderFactor(SurrenderFactorType.DIVIDEND_ACCOUNT_VALUE, ChargeDirection.NONE,
                        new BigDecimal("0.01"), "surrender.factor.dividendAccountValue"));

        assertEquals(ProductErrorCode.SURRENDER_FACTOR_INVALID, ex.getErrorCode());
    }

    @Test
    @DisplayName("方向 NONE 且金额为 0 合法：用于表达「本产品不适用该因子」")
    void shouldAcceptZeroAmountWhenDirectionIsNone() {
        SurrenderFactor factor = none();

        assertEquals(0, factor.amount().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("类型 / 方向 / 金额任一为空即拒绝")
    void shouldRejectNullComponents() {
        assertThrows(PricingDomainException.class, () -> new SurrenderFactor(null, ChargeDirection.CREDIT,
                BigDecimal.TEN, "surrender.factor.cashValue"));
        assertThrows(PricingDomainException.class, () -> new SurrenderFactor(SurrenderFactorType.CASH_VALUE,
                null, BigDecimal.TEN, "surrender.factor.cashValue"));
        assertThrows(PricingDomainException.class, () -> new SurrenderFactor(SurrenderFactorType.CASH_VALUE,
                ChargeDirection.CREDIT, null, "surrender.factor.cashValue"));
    }

    private static SurrenderFactor credit(BigDecimal amount) {
        return new SurrenderFactor(SurrenderFactorType.CASH_VALUE, ChargeDirection.CREDIT, amount,
                "surrender.factor.cashValue");
    }

    private static SurrenderFactor debit(BigDecimal amount) {
        return new SurrenderFactor(SurrenderFactorType.HANDLING_FEE, ChargeDirection.DEBIT, amount,
                "surrender.factor.handlingFee");
    }

    private static SurrenderFactor none() {
        return new SurrenderFactor(SurrenderFactorType.DIVIDEND_ACCOUNT_VALUE, ChargeDirection.NONE,
                BigDecimal.ZERO, "surrender.factor.dividendAccountValue");
    }
}
