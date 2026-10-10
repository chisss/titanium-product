package com.titanium.product.pricing.aggregate.loan;

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
 * 保单贷款策略领域对象测试（g04-03）。
 * <p>
 * 锁定生命周期（DRAFT→APPROVED→PUBLISHED→RETIRED）、内容哈希产生时机与
 * 比例/利率的 (0,1] 取值不变量——放款时冻结进贷款账户的正是这两个数，
 * 取值越界必须在配置发布环节被拦下而非放款时才发现。
 * </p>
 */
class PolicyLoanPolicyTest {

    private static final LocalDateTime EFFECTIVE_FROM = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Test
    void shouldWalkLifecycleFromDraftToRetired() {
        PolicyLoanPolicy policy = draftPolicy();

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
        PolicyLoanPolicy draft = draftPolicy();
        assertThrows(PricingDomainException.class, draft::publish);
        assertThrows(PricingDomainException.class, draft::retire);

        PolicyLoanPolicy approved = draftPolicy();
        approved.approve();
        assertThrows(PricingDomainException.class, approved::retire);
    }

    @Test
    void shouldRejectRatioOrRateOutOfRange() {
        assertThrows(PricingDomainException.class,
                () -> draftPolicy("1.2", "0.05"));
        assertThrows(PricingDomainException.class,
                () -> draftPolicy("0.8", "-0.01"));
        assertThrows(PricingDomainException.class,
                () -> draftPolicy(null, "0.05"));
    }

    @Test
    void shouldOnlyBeEffectiveWithinEffectiveWindow() {
        PolicyLoanPolicy policy = draftPolicy();
        policy.approve();
        policy.publish();

        // 生效时间（含）之后、失效时间（不含）之前适用
        assertTrue(policy.isEffectiveAt(EFFECTIVE_FROM));
        assertTrue(policy.isEffectiveAt(EFFECTIVE_FROM.plusMonths(11).plusDays(29)));
        assertFalse(policy.isEffectiveAt(EFFECTIVE_FROM.minusSeconds(1)));
        assertFalse(policy.isEffectiveAt(EFFECTIVE_FROM.plusYears(1)));
        assertFalse(policy.isEffectiveAt(null));
    }

    @Test
    void shouldRestorePublishedPolicyWithContentHash() {
        PolicyLoanPolicy restored = PolicyLoanPolicy.restore(
                "loan-1", "product-1", "LOAN-2026", "V1", new BigDecimal("0.80"), new BigDecimal("0.05"),
                EFFECTIVE_FROM, null, "tenant-a", ActuarialDefinitionStatus.PUBLISHED,
                "a".repeat(64));

        assertEquals("LOAN-2026", restored.getPolicyCode());
        assertEquals(0, new BigDecimal("0.80").compareTo(restored.getLoanableRatio()));
        assertEquals(0, new BigDecimal("0.05").compareTo(restored.getInterestRate()));
        // 已发布且无失效时间：当前时点适用
        assertTrue(restored.isEffectiveAt(LocalDateTime.now()));
    }

    private PolicyLoanPolicy draftPolicy() {
        return draftPolicy("0.80", "0.05");
    }

    private PolicyLoanPolicy draftPolicy(String ratio, String rate) {
        return PolicyLoanPolicy.createDraft(
                "loan-1", "product-1", "LOAN-2026", "V1",
                ratio == null ? null : new BigDecimal(ratio),
                rate == null ? null : new BigDecimal(rate),
                EFFECTIVE_FROM, EFFECTIVE_FROM.plusYears(1), "tenant-a");
    }
}
