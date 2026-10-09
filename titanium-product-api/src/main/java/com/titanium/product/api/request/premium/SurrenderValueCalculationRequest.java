package com.titanium.product.api.request.premium;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 基于原确认计算确认退保价值的请求。
 *
 * @param annualPremium            年保费（未满期保费基准所需，由保单域下发；现金价值基准可空）
 * @param coverageEndDate          保障止期（未满期保费按日折算终点；终身型可空）
 * @param premiumPaidUp            缴费是否期满（含趸缴）：期满即无未满期保费，该项恒为 0
 * @param loanOutstandingPrincipal 贷款未偿本金（退费时先行抵扣；无未结贷款为 0，取不到为 null）
 * @param loanOutstandingInterest  贷款未偿利息（退费时先行抵扣；无未结贷款为 0，取不到为 null）
 * @param overduePremium           宽限期内欠缴保费（退费时先行抵扣；无欠费为 0，取不到为 null）
 * @param dividendBalance          红利账户余额（退费时一并退还；未参与分红为 0，取不到为 null）
 * @param accountValue             投资账户价值（退费时一并退还；未挂接账户为 0，取不到为 null）
 */
public record SurrenderValueCalculationRequest(
        @NotBlank String surrenderRequestId,
        @NotBlank String bizNo,
        @NotBlank String originalBizNo,
        @NotBlank String originalCalculationId,
        @NotNull LocalDate policyEffectiveDate,
        @NotNull LocalDate surrenderDate,
        @NotNull @Min(1) Integer policyYear,
        @NotNull LocalDateTime businessTime,
        @NotBlank String reason,
        @DecimalMin("0") BigDecimal annualPremium,
        LocalDate coverageEndDate,
        boolean premiumPaidUp,
        @DecimalMin("0") BigDecimal loanOutstandingPrincipal,
        @DecimalMin("0") BigDecimal loanOutstandingInterest,
        @DecimalMin("0") BigDecimal overduePremium,
        @DecimalMin("0") BigDecimal dividendBalance,
        @DecimalMin("0") BigDecimal accountValue) {

    /**
     * 兼容构造器：不含保费事实、贷款事实与账户余额事实（均按「事实不可得」处理）。
     * <p>
     * 供既有调用方使用；缺事实时产品域在使用点失败关闭，不会静默按 0 计——「取不到」与
     * 「确定为 0」必须区分。
     * </p>
     */
    public SurrenderValueCalculationRequest(
            String surrenderRequestId,
            String bizNo,
            String originalBizNo,
            String originalCalculationId,
            LocalDate policyEffectiveDate,
            LocalDate surrenderDate,
            Integer policyYear,
            LocalDateTime businessTime,
            String reason) {
        this(surrenderRequestId, bizNo, originalBizNo, originalCalculationId, policyEffectiveDate,
                surrenderDate, policyYear, businessTime, reason, null, null, false, null, null,
                null, null, null);
    }
}
