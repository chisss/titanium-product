package com.titanium.product.web.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.titanium.common.context.RequestContextInterceptor;
import com.titanium.metadata.enums.pricing.ChargeDirection;
import com.titanium.product.application.command.pricing.surrender.SurrenderValueCommandAppService;
import com.titanium.product.application.model.pricing.surrender.SurrenderValueCalculationResult;
import com.titanium.product.application.query.pricing.surrender.CashValueQueryAppService;
import com.titanium.product.command.pricing.surrender.CalculateSurrenderValueCommand;
import com.titanium.product.common.enums.PremiumBalanceDirection;
import com.titanium.product.common.enums.SurrenderFactorType;
import com.titanium.product.common.enums.SurrenderRefundBasis;
import com.titanium.product.common.enums.SurrenderRefundType;
import com.titanium.product.pricing.aggregate.lifecycle.PremiumLifecycleAdjustment;
import com.titanium.product.valueobject.pricing.premium.SurrenderFactor;
import com.titanium.product.valueobject.pricing.premium.SurrenderValueOutcome;
import com.titanium.product.web.handler.ProductExceptionHandler;

/**
 * Product 退保价值远程契约出参测试（g05-01）
 * <p>
 * 🔴 断言点是<b>跨域契约字段真的被写进响应</b>：对端（保全域）按 {@code factors[]} 承接退费明细，
 * 若此处漏映射，对端适配器再完备也只能收到空明细——这类静默失败在编译期与既有用例中都不显形，
 * 只有真机对账时才暴露。故按 JSON 字段逐个断言，而非只断言状态码。
 * </p>
 */
class ProductSurrenderValueApiProviderTest {

    private SurrenderValueCommandAppService commandAppService;
    private CashValueQueryAppService cashValueQueryAppService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        commandAppService = mock(SurrenderValueCommandAppService.class);
        cashValueQueryAppService = mock(CashValueQueryAppService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProductSurrenderValueApiProvider(commandAppService, cashValueQueryAppService))
                .setControllerAdvice(new ProductExceptionHandler())
                .addInterceptors(new RequestContextInterceptor())
                .build();
    }

    @Test
    @DisplayName("退保试算响应携带退费因子明细：类型码 / 方向码 / 金额 / 文案键逐项可读")
    void shouldExposeFactorDetailInResponse() throws Exception {
        SurrenderValueCalculationResult result = result(List.of(
                new SurrenderFactor(SurrenderFactorType.CASH_VALUE, ChargeDirection.CREDIT,
                        new BigDecimal("72.72"), SurrenderValueOutcome.EXPLAIN_CASH_VALUE),
                new SurrenderFactor(SurrenderFactorType.LOAN_PRINCIPAL_INTEREST, ChargeDirection.DEBIT,
                        new BigDecimal("10.00"), "surrender.factor.loanPrincipalInterest")));
        when(commandAppService.calculate(any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/premium-calculations/surrender-values")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.factors.length()").value(2))
                .andExpect(jsonPath("$.data.factors[0].factorType").value("CASH_VALUE"))
                .andExpect(jsonPath("$.data.factors[0].direction").value("CREDIT"))
                .andExpect(jsonPath("$.data.factors[0].explainKey").value("surrender.factor.cashValue"))
                .andExpect(jsonPath("$.data.factors[1].factorType").value("LOAN_PRINCIPAL_INTEREST"))
                .andExpect(jsonPath("$.data.factors[1].direction").value("DEBIT"))
                .andExpect(jsonPath("$.data.refundBasis").value("CASH_VALUE"))
                .andExpect(jsonPath("$.data.refundAmount").value(72.72));
    }

    @Test
    @DisplayName("保费事实随请求透传到命令：跨域契约新增分量不得被适配层静默丢弃")
    void shouldPassPremiumFactsThroughToCommand() throws Exception {
        SurrenderValueCalculationResult calculationResult = result(List.of(
                new SurrenderFactor(SurrenderFactorType.UNEARNED_PREMIUM, ChargeDirection.CREDIT,
                        new BigDecimal("109.59"), SurrenderValueOutcome.EXPLAIN_UNEARNED_PREMIUM)));
        when(commandAppService.calculate(any())).thenReturn(calculationResult);

        mockMvc.perform(post("/api/v1/premium-calculations/surrender-values")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isOk());

        ArgumentCaptor<CalculateSurrenderValueCommand> captor =
                ArgumentCaptor.forClass(CalculateSurrenderValueCommand.class);
        verify(commandAppService).calculate(captor.capture());
        assertEquals(0, new BigDecimal("200.00").compareTo(captor.getValue().annualPremium()),
                "期望 200.00，实际 " + captor.getValue().annualPremium());
        assertEquals(LocalDate.of(2027, 3, 8), captor.getValue().coverageEndDate());
        assertTrue(captor.getValue().premiumPaidUp());
    }

    @Test
    @DisplayName("无适用策略：返回业务码失败而非 HTTP 错误，供上游降级为「取不到现金价值」")
    void shouldReturnBusinessFailureWhenNoPublishedPolicy() throws Exception {
        when(cashValueQueryAppService.query(any(), any(), any(Integer.class), any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/surrender-value-policies/cash-value")
                        .header("X-Tenant-Id", "1")
                        .param("productId", "product-1")
                        .param("policyYear", "2")
                        .param("customerPayable", "121.20")
                        .param("policyEffectiveDate", "2026-01-01")
                        .param("valuationDate", "2026-08-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("60000170"));
    }

    private static String requestBody() {
        return """
                {
                  "surrenderRequestId": "surrender-req-1",
                  "bizNo": "maintenance-1",
                  "originalBizNo": "issuance-biz-1",
                  "originalCalculationId": "calc-original",
                  "policyEffectiveDate": "2026-01-01",
                  "surrenderDate": "2026-08-20",
                  "policyYear": 1,
                  "businessTime": "2026-08-20T12:00:00",
                  "reason": "LIFE_SURRENDER",
                  "annualPremium": 200.00,
                  "coverageEndDate": "2027-03-08",
                  "premiumPaidUp": true
                }
                """;
    }

    private static SurrenderValueCalculationResult result(List<SurrenderFactor> factors) {
        PremiumLifecycleAdjustment adjustment = mock(PremiumLifecycleAdjustment.class);
        when(adjustment.getOriginalCalculationId()).thenReturn("calc-original");
        when(adjustment.getReplacementCalculationId()).thenReturn("calc-replacement");
        when(adjustment.getAdjustmentId()).thenReturn("adjustment-1");
        when(adjustment.getResultHash()).thenReturn("hash-adjustment");
        when(adjustment.getDirection()).thenReturn(PremiumBalanceDirection.CREDIT);
        when(adjustment.getCustomerAmount()).thenReturn(new BigDecimal("72.72"));
        when(adjustment.getCurrency()).thenReturn("CNY");
        return new SurrenderValueCalculationResult(
                "surrender-req-1", "LIFE-SURRENDER-CASH-VALUE", "V1.0", "hash-policy", 1, 15,
                SurrenderRefundBasis.CASH_VALUE, SurrenderRefundType.CASH_VALUE, false,
                new BigDecimal("0.60000000"), new BigDecimal("72.72"),
                new BigDecimal("48.48"), BigDecimal.ZERO, factors,
                "hash-request", "hash-original", "hash-replacement", "P1", "hash-plan", adjustment);
    }
}
