package com.titanium.product.command.pricing.surrender;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 确认退保价值并生成生命周期差额的应用命令。
 *
 * @param annualPremium             年保费（未满期保费基准所需，由保单域下发；现金价值基准可空）
 * @param coverageEndDate           保障止期（未满期保费按日折算的终点；终身型可空）
 * @param premiumPaidUp             缴费是否期满（含趸缴）：期满即无未满期保费，该项恒为 0
 * @param loanOutstandingPrincipal  贷款未偿本金（退费时先行抵扣；无未结贷款为 0，取不到为 null）
 * @param loanOutstandingInterest   贷款未偿利息（退费时先行抵扣；无未结贷款为 0，取不到为 null）
 * @param overduePremium            宽限期内欠缴保费（退费时先行抵扣；无欠费为 0，取不到为 null）
 * @param dividendBalance           红利账户余额（退费时一并退还；未参与分红为 0，取不到为 null）
 * @param accountValue              投资账户价值（退费时一并退还；未挂接账户为 0，取不到为 null）
 */
public record CalculateSurrenderValueCommand(
        String tenantId,
        String surrenderRequestId,
        String bizNo,
        String originalBizNo,
        String originalCalculationId,
        LocalDate policyEffectiveDate,
        LocalDate surrenderDate,
        Integer policyYear,
        LocalDateTime businessTime,
        String reason,
        BigDecimal annualPremium,
        LocalDate coverageEndDate,
        boolean premiumPaidUp,
        BigDecimal loanOutstandingPrincipal,
        BigDecimal loanOutstandingInterest,
        BigDecimal overduePremium,
        BigDecimal dividendBalance,
        BigDecimal accountValue) {

    /**
     * 兼容构造器：不含保费事实、贷款事实与账户余额事实（均按「事实不可得」处理）。
     * <p>
     * 仅供既有用例使用；退费基准为未满期保费时缺保费事实、正式退费时缺贷款事实或账户余额事实，
     * 策略都会在使用点失败关闭（{@code SURRENDER_PREMIUM_FACTS_MISSING} /
     * {@code SURRENDER_LOAN_FACTS_MISSING} / {@code SURRENDER_BALANCE_FACTS_MISSING}），
     * 不会静默按 0 计。
     * </p>
     */
    public CalculateSurrenderValueCommand(
            String tenantId,
            String surrenderRequestId,
            String bizNo,
            String originalBizNo,
            String originalCalculationId,
            LocalDate policyEffectiveDate,
            LocalDate surrenderDate,
            Integer policyYear,
            LocalDateTime businessTime,
            String reason) {
        this(tenantId, surrenderRequestId, bizNo, originalBizNo, originalCalculationId, policyEffectiveDate,
                surrenderDate, policyYear, businessTime, reason, null, null, false, null, null,
                null, null, null);
    }
}
