package com.titanium.product.application.query.pricing.paidup;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.titanium.product.application.model.pricing.paidup.ReducedPaidUpPolicySnapshot;
import com.titanium.product.pricing.aggregate.paidup.ReducedPaidUpPolicy;
import com.titanium.product.repository.ReducedPaidUpPolicyRepository;

import lombok.RequiredArgsConstructor;

/**
 * 减额缴清净费率策略只读查询应用服务（application/query，g04-05）。
 * <p>
 * 只做「查业务时点适用的已发布减额缴清策略」这一读路径，不落库、零副作用。
 * 保全域减额缴清编排取到每元保额趸缴净费率后即可核定新保额（{@code 现金价值 ÷ 费率}），
 * 本服务不再参与后续核算。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ReducedPaidUpPolicyQueryAppService {

    private final ReducedPaidUpPolicyRepository reducedPaidUpPolicyRepository;

    /**
     * 查业务时点适用的已发布减额缴清策略。
     *
     * @param tenantId 租户ID
     * @param productId 产品ID
     * @param businessTime 业务时点（定位适用策略版本；为空取当前时间）
     * @return 策略快照；该时点无已发布策略时为空（由调用方决定降级语义）
     */
    public Optional<ReducedPaidUpPolicySnapshot> query(
            String tenantId, String productId, LocalDateTime businessTime) {
        LocalDateTime at = businessTime != null ? businessTime : LocalDateTime.now();
        return reducedPaidUpPolicyRepository.findPublished(tenantId, productId, at)
                .map(this::toSnapshot);
    }

    private ReducedPaidUpPolicySnapshot toSnapshot(ReducedPaidUpPolicy policy) {
        return new ReducedPaidUpPolicySnapshot(
                policy.getPolicyId(), policy.getProductId(), policy.getPolicyCode(), policy.getPolicyVersion(),
                policy.getNetSinglePremiumRatePerUnit(), policy.getEffectiveFrom(),
                policy.getEffectiveTo(), policy.getContentHash());
    }
}
