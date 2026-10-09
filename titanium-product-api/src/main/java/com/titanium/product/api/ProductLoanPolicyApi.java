package com.titanium.product.api;

import java.time.LocalDateTime;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.response.loan.LoanPolicyQueryResponse;

/** Product 保单贷款策略远程契约（g04-03）。 */
@FeignClient(name = "titanium-product-service", contextId = "productLoanPolicyApi", path = "/api/v1")
public interface ProductLoanPolicyApi {

    /**
     * 保单贷款策略只读查询（纯查表，零副作用）。
     * <p>
     * 按业务时点返回适用的已发布贷款策略（可贷比例 + 年利率），供保全域放款编排冻结进贷款账户。
     * </p>
     *
     * @param productId 产品ID
     * @param businessTime 业务时点（定位适用策略版本；可为空，空则取当前时间）
     * @return 贷款策略；该时点无已发布策略时业务码非成功、{@code data} 为空
     */
    @GetMapping("/loan-policies/effective")
    ApiResponse<LoanPolicyQueryResponse> queryEffectiveLoanPolicy(@RequestParam String productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime businessTime);
}
