package com.titanium.product.web.dto.pricing.surrender;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.titanium.product.common.enums.SurrenderRefundBasis;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建退保价值策略草稿请求。
 *
 * @param refundBasis     退费基准（{@code CASH_VALUE} 现金价值 / {@code UNEARNED_PREMIUM} 未满期保费）；
 *                        可空，缺省按现金价值处理以保持历史口径，一年期短期险须显式传未满期保费
 * @param handlingFeeRate 退保手续费率；**刻意不加 {@code @NotNull}**——null 表示该产品不收退保手续费
 *                        （不适用），与「配置为 0」是两种业务事实
 */
public record CreateSurrenderValuePolicyDTO(
        @NotBlank String policyCode,
        @NotBlank String policyVersion,
        @Min(1) int policyYear,
        @Min(0) int coolingOffDays,
        @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal cashValueRate,
        @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal internalCostRetentionRate,
        @DecimalMin("0") @DecimalMax("1") BigDecimal handlingFeeRate,
        SurrenderRefundBasis refundBasis,
        @NotNull LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo) {
}
