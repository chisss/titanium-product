package com.titanium.product.valueobject.pricing.ncd;

import java.time.LocalDateTime;

import com.titanium.metadata.errorcode.ProductErrorCode;
import com.titanium.product.exception.PricingDomainException;

/**
 * NCD 反算的历史赔案统计区间（**日粒度半开区间** {@code [from, to)}）
 * <p>
 * 由规则集的 {@code lookbackMonths} 与业务日共同派生（见
 * {@code NcdRuleSet#lookbackWindow(LocalDate)}）：{@code from} 为回看起点当日 0 时（**含**），
 * {@code to} 为业务日次日 0 时（**不含**）——故业务日当天发生的赔案**计入**统计。
 * </p>
 * <p>
 * 🔴 为什么必须单独承载而不是两个 {@code LocalDateTime} 到处传：区间同时是**取数入参**（claim 侧
 * 查询条件）与**取证字段**（AC-04 要随出单落库的「区间」）。若两处各自拼装，「查询用的区间」与
 * 「落库的区间」可以无声地分叉，取证字段就失去意义；且裸传两个时间戳极易上下界写反。
 * </p>
 * <p>
 * 🔴 与次数档位的**闭区间**口径不同（见 {@link com.titanium.product.pricing.aggregate.ncd.NcdRule}）：
 * 区间是连续时间（半开避免端点日记两次），次数是离散计数（闭区间避免恰好等于上界落空）。
 * 两者不可互相套用。
 * </p>
 *
 * @param from 统计起点（含）
 * @param to   统计止点（不含），必须晚于 {@code from}
 */
public record NcdLookbackWindow(LocalDateTime from, LocalDateTime to) {

    public NcdLookbackWindow {
        if (from == null || to == null) {
            throw invalid("统计区间的起止时间不能为空");
        }
        if (!to.isAfter(from)) {
            throw invalid("统计区间的止点必须晚于起点");
        }
    }

    private static PricingDomainException invalid(String detail) {
        return new PricingDomainException(ProductErrorCode.NCD_RULE_INVALID, detail);
    }
}
