package com.titanium.product.domain.valueobject;

import java.math.BigDecimal;
import java.util.List;

/**
 * 投保条件值对象
 * 表示产品的投保条件配置，包含年龄范围、职业限制、保额范围、地域限制等
 *
 * @param minAge               最小投保年龄
 * @param maxAge               最大投保年龄
 * @param forbiddenOccupations 禁止职业列表
 * @param allowedOccupations   允许职业列表（白名单模式）
 * @param minGroupSize         团险最小参保人数
 * @param maxGroupSize         团险最大参保人数
 * @param healthNotice         健康告知要求
 * @param minInsuredAmount     最小保额
 * @param maxInsuredAmount     最大保额
 * @param forbiddenRegions     禁止地域列表
 * @param allowedRegions       允许地域列表
 * @param maxInsuredCount      最大投保人数限制
 * @param waitingPeriodDays    等待期（天）
 * @param hesitationPeriodDays 犹豫期（天）
 */
public record InsureCondition(
        Integer minAge,
        Integer maxAge,
        List<String> forbiddenOccupations,
        List<String> allowedOccupations,
        Integer minGroupSize,
        Integer maxGroupSize,
        String healthNotice,
        BigDecimal minInsuredAmount,
        BigDecimal maxInsuredAmount,
        List<String> forbiddenRegions,
        List<String> allowedRegions,
        Integer maxInsuredCount,
        Integer waitingPeriodDays,
        Integer hesitationPeriodDays
) {
}
