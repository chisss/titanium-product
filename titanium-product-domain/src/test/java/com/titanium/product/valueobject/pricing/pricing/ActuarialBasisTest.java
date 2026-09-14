package com.titanium.product.valueobject.pricing.pricing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 精算基础参数值对象契约测试（m16-1904）
 * <p>
 * 本对象属<b>未落地契约</b>（字段已贯通写入/持久化/回放/后台回显，但全域无任何计算消费方，
 * 详见类注释）。此处固化其值域与回落语义，供未来精算计算接入方按此实现——避免各消费方
 * 自立口径，也避免「未配置」与「等于定价利率」两种语义被混同。
 * </p>
 */
class ActuarialBasisTest {

    @Test
    @DisplayName("三参工厂 of()：评估利率留空为 null，回落定价利率由消费方完成")
    void ofLeavesReserveValuationRateNull() {
        ActuarialBasis basis = ActuarialBasis.of(new BigDecimal("0.03"), "CL2010-2013",
                new BigDecimal("0.20"));

        assertNull(basis.reserveValuationRate(),
                "评估利率留空须为 null——本值对象不做隐式回落，否则消费方无从区分「未配置」与「等于定价利率」");
    }

    @Test
    @DisplayName("预定利率值域：开区间 (0,1) 内有效")
    void hasValidInterestRateAcceptsOpenInterval() {
        assertTrue(ActuarialBasis.of(new BigDecimal("0.03"), "CL2010-2013", null).hasValidInterestRate());
    }

    @Test
    @DisplayName("预定利率值域：0 / 1 / 负数 / null 均无效（不含端点）")
    void hasValidInterestRateRejectsBoundaryAndInvalid() {
        assertFalse(ActuarialBasis.of(BigDecimal.ZERO, "CL2010-2013", null).hasValidInterestRate());
        assertFalse(ActuarialBasis.of(BigDecimal.ONE, "CL2010-2013", null).hasValidInterestRate());
        assertFalse(ActuarialBasis.of(new BigDecimal("-0.01"), "CL2010-2013", null).hasValidInterestRate());
        assertFalse(ActuarialBasis.of(null, "CL2010-2013", null).hasValidInterestRate());
    }
}
