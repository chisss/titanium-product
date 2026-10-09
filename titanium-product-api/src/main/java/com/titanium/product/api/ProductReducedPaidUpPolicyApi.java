package com.titanium.product.api;

import java.time.LocalDateTime;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.response.paidup.ReducedPaidUpPolicyQueryResponse;

/** Product 减额缴清净费率策略远程契约（g04-05）。 */
@FeignClient(name = "titanium-product-service", contextId = "productReducedPaidUpPolicyApi", path = "/api/v1")
public interface ProductReducedPaidUpPolicyApi {

    /**
     * 减额缴清净费率策略只读查询（纯查表，零副作用）。
     * <p>
     * 按业务时点返回适用的已发布策略（每元保额趸缴净费率），供保全域减额缴清编排核定新保额
     * （{@code 新保额 = 现金价值 ÷ 费率}）。
     * </p>
     *
     * @param productId 产品ID
     * @param businessTime 业务时点（定位适用策略版本；可为空，空则取当前时间）
     * @return 策略；该时点无已发布策略时业务码非成功、{@code data} 为空
     */
    @GetMapping("/reduced-paid-up-policies/effective")
    ApiResponse<ReducedPaidUpPolicyQueryResponse> queryEffectiveReducedPaidUpPolicy(
            @RequestParam String productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime businessTime);
}
