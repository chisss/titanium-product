package com.titanium.product.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.request.premium.SurrenderValueCalculationRequest;
import com.titanium.product.api.response.premium.CashValueQueryResponse;
import com.titanium.product.api.response.premium.SurrenderValueCalculationResponse;

import jakarta.validation.Valid;

/** Product 退保价值确认远程契约。 */
@FeignClient(name = "titanium-product-service", contextId = "productSurrenderValueApi", path = "/api/v1")
public interface ProductSurrenderValueApi {

    @PostMapping("/premium-calculations/surrender-values")
    ApiResponse<SurrenderValueCalculationResponse> calculate(
            @Valid @RequestBody SurrenderValueCalculationRequest request);

    /**
     * 保单现金价值只读查询（纯查表计算，零副作用）。
     * <p>
     * 区别于 {@link #calculate}：不要求出单确认计算记录、不落库、不产生生命周期贷项，供上游按日扫描等
     * 高频只读场景取值；计算复用同一份 {@code SurrenderValuePolicy.calculate} 公式，口径与退保试算一致。
     * </p>
     *
     * @param productId 产品ID
     * @param policyYear 保单年度（自 1 起）
     * @param customerPayable 客户应付保费基数（上游传保单总保费）
     * @param policyEffectiveDate 保单生效日（犹豫期判定基准）
     * @param valuationDate 估值日
     * @param businessTime 业务时点（定位适用策略版本；可为空，空则取当前时间）
     * @return 现金价值；该时点无已发布退保价值策略时业务码非成功、{@code data} 为空
     */
    @GetMapping("/surrender-value-policies/cash-value")
    ApiResponse<CashValueQueryResponse> queryCashValue(@RequestParam String productId,
            @RequestParam int policyYear, @RequestParam BigDecimal customerPayable,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate policyEffectiveDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate valuationDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime businessTime);
}
