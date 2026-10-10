package com.titanium.product.pricing.aggregate.paidup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.titanium.product.common.enums.ActuarialDefinitionStatus;
import com.titanium.product.exception.PricingDomainException;

/**
 * 减额缴清净费率策略领域对象测试（g04-05）。
 * <p>
 * 锁定生命周期（DRAFT→APPROVED→PUBLISHED→RETIRED）、内容哈希产生时机与
 * 费率的「必须为正、<b>无上界</b>」取值不变量——减额缴清核定的新保额是
 * {@code 现金价值 ÷ 每元保额趸缴净费率}，费率取值越界必须在配置发布环节被拦下，
 * 且误把费率当「比例」加上界会把高龄保单的合法配置判为非法。
 * </p>
 */
class ReducedPaidUpPolicyTest {

    private static final LocalDateTime EFFECTIVE_FROM = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Test
    void shouldWalkLifecycleFromDraftToRetired() {
        ReducedPaidUpPolicy policy = draftPolicy();

        assertFalse(policy.isEffectiveAt(EFFECTIVE_FROM));
        String contentHash = policy.approve();
        assertEquals(ActuarialDefinitionStatus.APPROVED, policy.getStatus());
        assertEquals(64, contentHash.length());

        policy.publish();
        assertEquals(ActuarialDefinitionStatus.PUBLISHED, policy.getStatus());
        assertTrue(policy.isEffectiveAt(EFFECTIVE_FROM));
        // 失效时间为开区间上界：生效区间 [effectiveFrom, effectiveTo)
        assertTrue(policy.isEffectiveAt(EFFECTIVE_FROM.plusYears(1).minusSeconds(1)));
        assertFalse(policy.isEffectiveAt(EFFECTIVE_FROM.plusYears(1)));

        policy.retire();
        assertEquals(ActuarialDefinitionStatus.RETIRED, policy.getStatus());
        assertFalse(policy.isEffectiveAt(EFFECTIVE_FROM));
    }

    @Test
    void shouldRejectIllegalStateTransitions() {
        ReducedPaidUpPolicy draft = draftPolicy();
        assertThrows(PricingDomainException.class, draft::publish);
        assertThrows(PricingDomainException.class, draft::retire);

        ReducedPaidUpPolicy approved = draftPolicy();
        approved.approve();
        assertThrows(PricingDomainException.class, approved::retire);
    }

    @Test
    void shouldRejectNonPositiveRate() {
        assertThrows(PricingDomainException.class,
                () -> draftPolicy("0"));
        assertThrows(PricingDomainException.class,
                () -> draftPolicy("-0.5"));
        assertThrows(PricingDomainException.class,
                () -> draftPolicy(null));
    }

    @Test
    void shouldAcceptRateGreaterThanOne() {
        // 🔴 费率是「每元保额的趸缴净保费」而非比例：高龄/高保单年度下大于 1 属正常业务事实，
        // 误加「<= 1」上界会把合法配置判为非法。本条锁死该刻意缺失的上界。
        ReducedPaidUpPolicy policy = draftPolicy("1.25");

        assertEquals(0, new BigDecimal("1.25").compareTo(policy.getNetSinglePremiumRatePerUnit()));
        policy.approve();
        policy.publish();
        assertTrue(policy.isEffectiveAt(EFFECTIVE_FROM));
    }

    @Test
    void shouldOnlyBeEffectiveWithinEffectiveWindow() {
        ReducedPaidUpPolicy policy = draftPolicy();
        policy.approve();
        policy.publish();

        assertTrue(policy.isEffectiveAt(EFFECTIVE_FROM));
        assertTrue(policy.isEffectiveAt(EFFECTIVE_FROM.plusMonths(11).plusDays(29)));
        assertFalse(policy.isEffectiveAt(EFFECTIVE_FROM.minusSeconds(1)));
        assertFalse(policy.isEffectiveAt(EFFECTIVE_FROM.plusYears(1)));
        assertFalse(policy.isEffectiveAt(null));
    }

    @Test
    void shouldRejectEffectiveToNotLaterThanEffectiveFrom() {
        assertThrows(PricingDomainException.class, () -> ReducedPaidUpPolicy.createDraft(
                "paidup-1", "product-1", "PAIDUP-2026", "V1", new BigDecimal("0.6"),
                EFFECTIVE_FROM, EFFECTIVE_FROM, "tenant-a"));
    }

    @Test
    void shouldRestorePublishedPolicyWithContentHash() {
        ReducedPaidUpPolicy restored = ReducedPaidUpPolicy.restore(
                "paidup-1", "product-1", "PAIDUP-2026", "V1", new BigDecimal("0.60"),
                EFFECTIVE_FROM, null, "tenant-a", ActuarialDefinitionStatus.PUBLISHED,
                "a".repeat(64));

        assertEquals("PAIDUP-2026", restored.getPolicyCode());
        assertEquals(0, new BigDecimal("0.60").compareTo(restored.getNetSinglePremiumRatePerUnit()));
        assertTrue(restored.isEffectiveAt(LocalDateTime.now()));
    }

    private ReducedPaidUpPolicy draftPolicy() {
        return draftPolicy("0.60");
    }

    private ReducedPaidUpPolicy draftPolicy(String rate) {
        return ReducedPaidUpPolicy.createDraft(
                "paidup-1", "product-1", "PAIDUP-2026", "V1",
                rate == null ? null : new BigDecimal(rate),
                EFFECTIVE_FROM, EFFECTIVE_FROM.plusYears(1), "tenant-a");
    }
}
