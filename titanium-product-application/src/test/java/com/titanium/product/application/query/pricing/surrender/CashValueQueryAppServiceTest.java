package com.titanium.product.application.query.pricing.surrender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.titanium.product.application.model.pricing.surrender.CashValueSnapshot;
import com.titanium.product.common.enums.SurrenderRefundBasis;
import com.titanium.product.pricing.aggregate.surrender.SurrenderValuePolicy;
import com.titanium.product.repository.SurrenderValuePolicyRepository;

/**
 * 保单现金价值只读查询应用服务测试
 * <p>
 * 校验只读查询复用 {@code SurrenderValuePolicy.calculate} 这唯一一份公式：犹豫期内全额、超犹豫期按
 * 现金价值率折算，且结果为**纯查表计算**（不落库、不产生生命周期贷项）。无适用策略时返回空，
 * 由调用方决定降级语义。纯 mockito，不启动容器。
 * </p>
 */
class CashValueQueryAppServiceTest {

    private static final String TENANT_ID = "TENANT-A";
    private static final String PRODUCT_ID = "PRD-0001";
    private static final BigDecimal CUSTOMER_PAYABLE = new BigDecimal("12000.00");
    private static final LocalDate POLICY_EFFECTIVE_DATE = LocalDate.of(2026, 1, 1);

    /** 已发布的退保价值策略：第 2 保单年度，犹豫期 15 天，现金价值率 60%。 */
    private SurrenderValuePolicy publishedPolicy() {
        SurrenderValuePolicy policy = SurrenderValuePolicy.createDraft("SVP-1", PRODUCT_ID,
                "LIFE-SURRENDER-CASH-VALUE", "V1.0", 2, 15, new BigDecimal("0.60000000"), BigDecimal.ZERO, null,
                SurrenderRefundBasis.CASH_VALUE, LocalDateTime.of(2026, 1, 1, 0, 0), null, TENANT_ID);
        policy.approve();
        policy.publish();
        return policy;
    }

    private SurrenderValuePolicyRepository repositoryReturning(SurrenderValuePolicy policy) {
        SurrenderValuePolicyRepository repository = mock(SurrenderValuePolicyRepository.class);
        when(repository.findPublished(anyString(), anyString(), anyInt(), any()))
                .thenReturn(Optional.ofNullable(policy));
        return repository;
    }

    @Test
    @DisplayName("【核心】超犹豫期按现金价值率折算：基数 × 60%")
    void shouldDiscountByCashValueRatePastCoolingOff() {
        SurrenderValuePolicyRepository repository = repositoryReturning(publishedPolicy());

        Optional<CashValueSnapshot> snapshot = new CashValueQueryAppService(repository).query(TENANT_ID, PRODUCT_ID, 2,
                CUSTOMER_PAYABLE, POLICY_EFFECTIVE_DATE, LocalDate.of(2026, 8, 1), null);

        assertTrue(snapshot.isPresent());
        CashValueSnapshot value = snapshot.get();
        assertFalse(value.withinCoolingOff());
        assertEquals("CASH_VALUE", value.refundType());
        assertEquals(0, new BigDecimal("7200.00").compareTo(value.cashValue()));
        assertEquals(0, new BigDecimal("0.60000000").compareTo(value.cashValueRate()));
        assertEquals(2, value.policyYear());
    }

    @Test
    @DisplayName("犹豫期内全额退还：现金价值率视为 1，应退 = 基数")
    void shouldRefundFullWithinCoolingOff() {
        SurrenderValuePolicyRepository repository = repositoryReturning(publishedPolicy());

        Optional<CashValueSnapshot> snapshot = new CashValueQueryAppService(repository).query(TENANT_ID, PRODUCT_ID, 2,
                CUSTOMER_PAYABLE, POLICY_EFFECTIVE_DATE, LocalDate.of(2026, 1, 10), null);

        assertTrue(snapshot.isPresent());
        CashValueSnapshot value = snapshot.get();
        assertTrue(value.withinCoolingOff());
        assertEquals("COOLING_OFF_FULL_REFUND", value.refundType());
        assertEquals(0, CUSTOMER_PAYABLE.compareTo(value.cashValue()));
        assertEquals(0, BigDecimal.ONE.compareTo(value.cashValueRate()));
    }

    @Test
    @DisplayName("承载策略取证信息：金额可追溯到具体策略版本")
    void shouldCarryPolicyEvidence() {
        SurrenderValuePolicyRepository repository = repositoryReturning(publishedPolicy());

        CashValueSnapshot value = new CashValueQueryAppService(repository)
                .query(TENANT_ID, PRODUCT_ID, 2, CUSTOMER_PAYABLE, POLICY_EFFECTIVE_DATE, LocalDate.of(2026, 8, 1), null)
                .orElseThrow();

        assertEquals("LIFE-SURRENDER-CASH-VALUE", value.policyCode());
        assertEquals("V1.0", value.policyVersion());
        assertFalse(value.policyContentHash() == null || value.policyContentHash().isBlank(),
                "已发布策略须带内容哈希以供事后复核");
    }

    @Test
    @DisplayName("按业务时点定位策略版本：传入时点原样下发，不由服务另行取当前时间")
    void shouldQueryPublishedPolicyAtGivenBusinessTime() {
        SurrenderValuePolicyRepository repository = repositoryReturning(publishedPolicy());
        LocalDateTime businessTime = LocalDateTime.of(2026, 8, 1, 9, 30);

        new CashValueQueryAppService(repository).query(TENANT_ID, PRODUCT_ID, 2, CUSTOMER_PAYABLE,
                POLICY_EFFECTIVE_DATE, LocalDate.of(2026, 8, 1), businessTime);

        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).findPublished(eq(TENANT_ID), eq(PRODUCT_ID), eq(2), timeCaptor.capture());
        assertEquals(businessTime, timeCaptor.getValue());
    }

    @Test
    @DisplayName("业务时点缺省取当前时间：日扫描等即时场景无需显式传参")
    void shouldDefaultBusinessTimeToNow() {
        SurrenderValuePolicyRepository repository = repositoryReturning(publishedPolicy());

        new CashValueQueryAppService(repository).query(TENANT_ID, PRODUCT_ID, 2, CUSTOMER_PAYABLE,
                POLICY_EFFECTIVE_DATE, LocalDate.of(2026, 8, 1), null);

        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).findPublished(anyString(), anyString(), anyInt(), timeCaptor.capture());
        assertFalse(timeCaptor.getValue().isBefore(LocalDateTime.now().minusMinutes(1)),
                "缺省业务时点应接近当前时间");
    }

    @Test
    @DisplayName("无适用策略：返回空，不伪造金额")
    void shouldReturnEmptyWhenNoPublishedPolicy() {
        SurrenderValuePolicyRepository repository = repositoryReturning(null);

        Optional<CashValueSnapshot> snapshot = new CashValueQueryAppService(repository).query(TENANT_ID, PRODUCT_ID, 9,
                CUSTOMER_PAYABLE, POLICY_EFFECTIVE_DATE, LocalDate.of(2026, 8, 1), null);

        assertTrue(snapshot.isEmpty());
    }
}
