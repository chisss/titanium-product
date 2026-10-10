package com.titanium.product.web.provider;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.common.context.RequestContextHolder;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.ProductReducedPaidUpPolicyApi;
import com.titanium.product.api.response.paidup.ReducedPaidUpPolicyQueryResponse;
import com.titanium.product.application.model.pricing.paidup.ReducedPaidUpPolicySnapshot;
import com.titanium.product.application.query.pricing.paidup.ReducedPaidUpPolicyQueryAppService;

import lombok.RequiredArgsConstructor;

/** Product 减额缴清净费率策略远程契约实现（g04-05）。 */
@Validated
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductReducedPaidUpPolicyApiProvider implements ProductReducedPaidUpPolicyApi {

    private final ReducedPaidUpPolicyQueryAppService reducedPaidUpPolicyQueryAppService;

    @Override
    public ApiResponse<ReducedPaidUpPolicyQueryResponse> queryEffectiveReducedPaidUpPolicy(
            String productId, LocalDateTime businessTime) {
        Optional<ReducedPaidUpPolicySnapshot> snapshot = reducedPaidUpPolicyQueryAppService.query(
                RequestContextHolder.requireTenantId(), productId, businessTime);
        // 该时点无已发布减额缴清策略：返回业务码失败（非 HTTP 错误），上游据此降级为「取不到减额定价」
        return snapshot
                .map(value -> ApiResponse.success(new ReducedPaidUpPolicyQueryResponse(
                        value.policyId(), value.productId(), value.policyCode(), value.policyVersion(),
                        value.netSinglePremiumRatePerUnit(), value.effectiveFrom(), value.effectiveTo(),
                        value.policyContentHash())))
                .orElseGet(() -> ApiResponse.error(ProductErrorCode.PRODUCT_REDUCED_PAID_UP_POLICY_NOT_FOUND,
                        "产品 " + productId + " 无适用的已发布减额缴清净费率策略"));
    }
}
