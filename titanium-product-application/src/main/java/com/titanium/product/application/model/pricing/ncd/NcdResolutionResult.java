package com.titanium.product.application.model.pricing.ncd;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * NCD 反算结果（含取证字段，G11/g11-03 AC-04）
 * <p>
 * 🔴 <b>取证字段是契约的一部分，不是可选附加</b>：{@code claimCount} / {@code periodFrom} /
 * {@code periodTo} / {@code ruleSetCode} / {@code ruleSetVersion} / {@code ruleSetContentHash}
 * 共同回答「这个系数是**依据什么**算出来的」。缺了它们，下游落库后无从复核，规则版本变更
 * 也无法区分新老单口径（出单须快照当时规则版本，与产品快照同法）。
 * </p>
 * <p>
 * 🔴 本结果**只由一次完整的反算产生**：取到已发布规则集 ∧ 取到历史赔案 ∧ 命中的唯一档位。
 * 任何一环取不到都在此之前显式失败（{@code NCD_RULE_NOT_CONFIGURED} /
 * {@code NCD_HISTORY_UNAVAILABLE} / {@code NCD_SUBJECT_IDENTIFIER_REQUIRED} /
 * {@code NCD_BAND_NOT_MATCHED} / {@code NCD_RULE_CONFLICT}），**不存在「带默认系数 1.0 的结果」**。
 * </p>
 *
 * @param productId           产品ID
 * @param coefficient         反算得到的 NCD 系数（取自命中档位，非默认值）
 * @param claimCount          统计区间内计入的历史赔案条数（取证：条数）
 * @param periodFrom          统计区间起点（含，取证：区间）
 * @param periodTo            统计区间止点（不含，取证：区间）
 * @param ruleSetCode         所用规则集编码（取证：规则版本）
 * @param ruleSetVersion      所用规则集版本（取证：规则版本）
 * @param ruleSetContentHash  所用规则集内容指纹（取证：内容未变则版本可比）
 */
public record NcdResolutionResult(
        String productId,
        BigDecimal coefficient,
        int claimCount,
        LocalDateTime periodFrom,
        LocalDateTime periodTo,
        String ruleSetCode,
        String ruleSetVersion,
        String ruleSetContentHash) {
}
