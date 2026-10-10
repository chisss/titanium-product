package com.titanium.product.web.provider;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.titanium.common.context.RequestContextHolder;
import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.metadata.response.ApiResponse;
import com.titanium.product.api.ProductLoanPolicyApi;
import com.titanium.product.api.response.loan.LoanPolicyQueryResponse;
import com.titanium.product.application.model.pricing.loan.LoanPolicySnapshot;
import com.titanium.product.application.query.pricing.loan.LoanPolicyQueryAppService;

import lombok.RequiredArgsConstructor;

/** Product 保单贷款策略远程契约实现（g04-03）。 */
@Validated
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductLoanPolicyApiProvider implements ProductLoanPolicyApi {

    private final LoanPolicyQueryAppService loanPolicyQueryAppService;

    @Override
    public ApiResponse<LoanPolicyQueryResponse> queryEffectiveLoanPolicy(
            String productId, LocalDateTime businessTime) {
        Optional<LoanPolicySnapshot> snapshot = loanPolicyQueryAppService.query(
                RequestContextHolder.requireTenantId(), productId, businessTime);
        // 该时点无已发布贷款策略：返回业务码失败（非 HTTP 错误），上游据此降级为「取不到放款定价」
        return snapshot
                .map(value -> ApiResponse.success(new LoanPolicyQueryResponse(
                        value.policyId(), value.productId(), value.policyCode(), value.policyVersion(),
                        value.loanableRatio(), value.interestRate(), value.effectiveFrom(), value.effectiveTo(),
                        value.policyContentHash())))
                .orElseGet(() -> ApiResponse.error(ProductErrorCode.PRODUCT_LOAN_POLICY_NOT_FOUND,
                        "产品 " + productId + " 无适用的已发布保单贷款策略"));
    }
}
