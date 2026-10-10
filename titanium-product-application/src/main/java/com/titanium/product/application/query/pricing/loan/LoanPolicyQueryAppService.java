package com.titanium.product.application.query.pricing.loan;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.titanium.product.application.model.pricing.loan.LoanPolicySnapshot;
import com.titanium.product.pricing.aggregate.loan.PolicyLoanPolicy;
import com.titanium.product.repository.PolicyLoanPolicyRepository;

import lombok.RequiredArgsConstructor;

/**
 * 保单贷款策略只读查询应用服务（application/query，g04-03）。
 * <p>
 * 只做「查业务时点适用的已发布贷款策略」这一读路径，不落库、零副作用。
 * 保全域放款编排取到比例/利率后冻结进贷款账户，本服务不再参与后续计息。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class LoanPolicyQueryAppService {

    private final PolicyLoanPolicyRepository policyLoanPolicyRepository;

    /**
     * 查业务时点适用的已发布贷款策略。
     *
     * @param tenantId 租户ID
     * @param productId 产品ID
     * @param businessTime 业务时点（定位适用策略版本；为空取当前时间）
     * @return 贷款策略快照；该时点无已发布策略时为空（由调用方决定降级语义）
     */
    public Optional<LoanPolicySnapshot> query(String tenantId, String productId, LocalDateTime businessTime) {
        LocalDateTime at = businessTime != null ? businessTime : LocalDateTime.now();
        return policyLoanPolicyRepository.findPublished(tenantId, productId, at)
                .map(this::toSnapshot);
    }

    private LoanPolicySnapshot toSnapshot(PolicyLoanPolicy policy) {
        return new LoanPolicySnapshot(
                policy.getPolicyId(), policy.getProductId(), policy.getPolicyCode(), policy.getPolicyVersion(),
                policy.getLoanableRatio(), policy.getInterestRate(), policy.getEffectiveFrom(),
                policy.getEffectiveTo(), policy.getContentHash());
    }
}
