package com.titanium.product.application.query.pricing.surrender;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.titanium.product.application.model.pricing.surrender.CashValueSnapshot;
import com.titanium.product.pricing.aggregate.surrender.SurrenderValuePolicy;
import com.titanium.product.repository.SurrenderValuePolicyRepository;
import com.titanium.product.valueobject.pricing.premium.SurrenderValueOutcome;

import lombok.RequiredArgsConstructor;

/**
 * 保单现金价值只读查询应用服务（application/query）。
 * <p>
 * 与 {@code SurrenderValueApplicationService}（application/orchestration，退保价值**确认**）的分工：
 * 本服务只做「查已发布退保价值策略 → 调其纯计算」这一读路径，**不落库、不创建生命周期贷项、不要求出单
 * 确认计算记录**，供上游按日扫描类高频只读场景使用；退保确认（有副作用、需凭证与幂等）仍走编排器。
 * </p>
 * <p>
 * 计算复用 {@link SurrenderValuePolicy#calculate} 这唯一一份公式，避免在调用方另立口径造成现金价值
 * 与退保试算结果漂移。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class CashValueQueryAppService {

    /** 金额精度（元，两位小数） */
    private static final int DEFAULT_SCALE = 2;

    private final SurrenderValuePolicyRepository surrenderValuePolicyRepository;

    /**
     * 查保单现金价值：按业务时点取适用的已发布退保价值策略，以传入保费基数算出应退金额。
     *
     * @param tenantId 租户ID
     * @param productId 产品ID
     * @param policyYear 保单年度（自 1 起）
     * @param customerPayable 客户应付保费基数（保单域传其保单总保费，与退保试算口径一致）
     * @param policyEffectiveDate 保单生效日（犹豫期判定基准）
     * @param valuationDate 估值日（超犹豫期则按现金价值率折算）
     * @param businessTime 业务时点（定位适用策略版本；为空取当前时间）
     * @return 现金价值快照；该时点无已发布策略时为空（由调用方决定降级语义）
     */
    public Optional<CashValueSnapshot> query(String tenantId, String productId, int policyYear,
            BigDecimal customerPayable, LocalDate policyEffectiveDate, LocalDate valuationDate,
            LocalDateTime businessTime) {
        LocalDateTime at = businessTime != null ? businessTime : LocalDateTime.now();
        return surrenderValuePolicyRepository.findPublished(tenantId, productId, policyYear, at)
                .map(policy -> toSnapshot(policy, customerPayable, policyEffectiveDate, valuationDate));
    }

    /**
     * 策略 → 现金价值快照：委托策略自身计算，仅补齐取证字段。
     */
    private CashValueSnapshot toSnapshot(SurrenderValuePolicy policy, BigDecimal customerPayable,
            LocalDate policyEffectiveDate, LocalDate valuationDate) {
        SurrenderValueOutcome outcome = policy.calculate(customerPayable, policyEffectiveDate, valuationDate,
                DEFAULT_SCALE, RoundingMode.HALF_UP);
        return new CashValueSnapshot(policy.getProductId(), policy.getPolicyYear(), policy.getCoolingOffDays(),
                outcome.withinCoolingOff(), outcome.refundType().name(), outcome.cashValueRate(),
                outcome.refundAmount(), policy.getPolicyCode(), policy.getPolicyVersion(),
                policy.getContentHash());
    }
}
